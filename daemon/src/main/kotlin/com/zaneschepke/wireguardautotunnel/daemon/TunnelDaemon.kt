package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.wgtunnel.backend.system.DesktopFirewall
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonSession
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonSessionStore
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.log.DaemonLogService
import com.zaneschepke.wireguardautotunnel.daemon.plugin.hmacShieldPlugin
import com.zaneschepke.wireguardautotunnel.daemon.routes.backendRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.daemonRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.tunnelRoutes
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DesktopNetworkMonitor
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.startFromRequest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

private val daemonLog = Logger.withTag("TunnelDaemon")

class TunnelDaemon(
    private val json: Json,
    private val backend: Backend,
    private val cacheRepository: DaemonCacheRepository,
    private val socketPath: String,
    private val autoTunnelSupervisor: AutoTunnelSupervisor,
    private val networkMonitor: DesktopNetworkMonitor? = null,
    private val daemonLogService: DaemonLogService,
    private val sessionStore: DaemonSessionStore,
    private val scope: CoroutineScope,
    // What EarlyKillSwitch enforced before the backend existed, so its status can match.
    private val earlyKillSwitch: KillSwitchConfigDto? = null,
) {
    private var server: EmbeddedServer<*, *>? = null
    private val running = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)
    private val restartRequested = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)

    // launchd, systemd and WinSW all set this and relaunch the daemon when it exits.
    private val supervised = System.getenv("WG_TUNNEL_SERVICE") == "1"

    internal fun run() {
        daemonLog.i { "Daemon starting on ${osName()}" }

        runBlocking {
            earlyKillSwitch?.let { config ->
                backend
                    .setKillSwitch(config.toCore())
                    .onSuccess { daemonLog.i { "Kill switch restored" } }
                    .onFailure { daemonLog.e(it) { "Failed to restore kill switch" } }
            }
            val bootstrapDnsMode = cacheRepository.getBootstrapDnsMode()
            if (bootstrapDnsMode != null) {
                daemonLog.i { "Restoring bootstrap DNS mode protocol=${bootstrapDnsMode.protocol}" }
                backend.setBootstrapDnsMode(bootstrapDnsMode.toCore())
            }
        }
        startUdsServer()
        daemonLog.i { "Binding Unix socket at: $socketPath" }
        sessionStore.loadFromThisBoot()?.let(::restoreSession)
        shutdownLatch.await()
    }

    /** Brings back what a daemon that exited earlier in this boot was running. */
    private fun restoreSession(session: DaemonSession) {
        val plan = session.autoTunnelPlan
        scope.launch {
            if (plan != null) autoTunnelSupervisor.updatePlan(plan)
            // Auto-tunnel picks the tunnel itself.
            if (plan?.enabled == true) {
                daemonLog.i { "Restored auto-tunnel after a daemon restart" }
                return@launch
            }
            session.tunnels.forEach { (id, request) ->
                daemonLog.i { "Restoring tunnel ${request.name} after a daemon restart" }
                // Bootstrap waits for a network and retries on its own, so one start is enough.
                autoTunnelSupervisor.tunnelActionMutex
                    .withLock {
                        if (id in backend.status.value.activeTunnels) Result.success(Unit)
                        else backend.startFromRequest(id, request, cacheRepository)
                    }
                    .onSuccess { sessionStore.putTunnel(id, request) }
                    .onFailure {
                        daemonLog.e(it) { "Failed to restore ${request.name}" }
                        sessionStore.removeTunnel(id)
                    }
            }
        }
    }

    fun startUdsServer() {
        if (!running.compareAndSet(false, true)) return

        daemonLog.i { "Starting IPC server" }

        val socketFile = File(socketPath)
        val runtimeDir = socketFile.parentFile
        runtimeDir.mkdirs()

        if (isWindows()) {
            PermissionsHelper.setupRuntimeDirectoryPermissionsWindows(runtimeDir.absolutePath)
        } else {
            PermissionsHelper.setupDirectoryPermissionsUnix(runtimeDir.absolutePath)
        }

        socketFile.delete()

        server =
            embeddedServer(CIO, configure = { unixConnector(socketPath) }) {
                    install(DoubleReceive)
                    install(ContentNegotiation) { json(json) }
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(json)
                        pingPeriodMillis = 20_000
                        timeoutMillis = 20_000
                        maxFrameSize = Long.MAX_VALUE
                    }
                    install(StatusPages) {
                        status(HttpStatusCode.NotFound) { call, status ->
                            call.respond(
                                status,
                                mapOf("error" to "Route not found", "path" to call.request.uri),
                            )
                        }

                        exception<Throwable> { call, cause ->
                            // Open websockets are canceled when the server stops, not an error.
                            if (cause is CancellationException) throw cause
                            daemonLog.e(cause) { "Unhandled exception in daemon" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to cause.message),
                            )
                        }
                    }
                    install(hmacShieldPlugin)
                    routing {
                        daemonRoutes(
                            cacheRepository,
                            autoTunnelSupervisor,
                            daemonLogService,
                            sessionStore,
                            backend,
                            networkMonitor,
                            restart = if (supervised) ::requestRestart else null,
                        )
                        tunnelRoutes(backend, autoTunnelSupervisor, cacheRepository, sessionStore)
                        backendRoutes(backend, cacheRepository)
                    }
                    monitor.subscribe(ApplicationStarted) {
                        daemonLog.i { "IPC server started successfully" }
                    }
                }
                .start(wait = false)

        scope.launch {
            if (isWindows()) {
                PermissionsHelper.setupSocketPermissionsWithPollWindows(socketPath)
            } else {
                PermissionsHelper.setupSocketPermissionsWithPollUnix(socketPath)
            }
        }
    }

    /**
     * Exits so the service manager starts the binary now on disk, after an app update replaced it.
     * The shutdown hook runs [stop] first. The non-zero code is what makes WinSW restart it.
     */
    private fun requestRestart() {
        if (!restartRequested.compareAndSet(false, true)) return
        daemonLog.i { "Restart requested, exiting for the service manager to relaunch" }
        scope.launch {
            // Lets the HTTP reply reach the client before the server goes down.
            delay(RESTART_REPLY_GRACE_MS.milliseconds)
            exitProcess(RESTART_EXIT_CODE)
        }
    }

    /** Runs from the shutdown hook, so it must finish within the service manager's stop timeout. */
    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        daemonLog.i { "Daemon stop initiated - closing all tunnels" }
        runCatching { autoTunnelSupervisor.stop() }
            .onFailure { daemonLog.e(it) { "Failed to stop auto-tunnel" } }
        runBlocking {
            val closed =
                withTimeoutOrNull(TUNNEL_STOP_TIMEOUT) {
                    runCatching {
                        sessionStore.retainTunnels(backend.status.value.activeTunnels.keys)
                        backend.stopAllActiveTunnels().getOrThrow()
                    }
                        .onFailure { daemonLog.e(it) { "Failed to close tunnels" } }
                }
            if (closed == null) daemonLog.e { "Timed out closing tunnels" }
            // Runs whatever happened above, or the kill switch is never kept across a reboot or
            // turned off.
            runCatching { settleKillSwitch() }
                .onFailure { daemonLog.e(it) { "Failed to settle the kill switch" } }
        }
        runCatching { networkMonitor?.stop() }
        daemonLog.i { "All tunnels closed - stopping server" }
        server?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        shutdownLatch.countDown()
        daemonLog.i { "UDS server fully stopped" }
    }

    // The user's kill switch outlives the daemon, so an update, crash or service stop never leaks.
    // Uninstall and the purge command remove it.
    private suspend fun settleKillSwitch() {
        if (cacheRepository.getKillSwitchEnabled()) {
            // With restore on boot, Windows keeps the rules through a reboot too. Linux and macOS
            // put them back at boot.
            val acrossReboot = cacheRepository.getKillSwitchRestore()
            if (DesktopFirewall.release(acrossReboot)) {
                daemonLog.i { "Kill switch left enforced (across reboot=$acrossReboot)" }
            } else daemonLog.e { "Failed to keep the kill switch enforced" }
        } else {
            backend.disableKillSwitch()
            DesktopFirewall.purge()
        }
    }

    private companion object {
        // WinSW gives a service 15s to stop and launchd 20s, so the kill switch gets the rest.
        val TUNNEL_STOP_TIMEOUT = 8.seconds
        const val RESTART_REPLY_GRACE_MS = 300L
        const val RESTART_EXIT_CODE = 75

        fun osName(): String = System.getProperty("os.name").orEmpty()

        fun isWindows(): Boolean = osName().startsWith("Windows", ignoreCase = true)
    }
}

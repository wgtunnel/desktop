package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.log.DaemonLogService
import com.zaneschepke.wireguardautotunnel.daemon.plugin.hmacShieldPlugin
import com.zaneschepke.wireguardautotunnel.daemon.routes.backendRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.daemonRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.tunnelRoutes
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DesktopNetworkMonitor
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val scope: CoroutineScope,
) {
    private var server: EmbeddedServer<*, *>? = null
    private val running = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)

    internal fun run() {
        daemonLog.i { "Daemon starting on ${osName()}" }

        runBlocking {
            val restoreKillSwitch = cacheRepository.getKillSwitchRestore()
            if (restoreKillSwitch && cacheRepository.getKillSwitchEnabled()) {
                val config = cacheRepository.getKillSwitchConfig()?.toCore()
                if (config != null) {
                    daemonLog.i { "Restoring kill switch from previous state" }
                    backend
                        .setKillSwitch(config)
                        .onFailure { daemonLog.e(it) { "Failed to restore kill switch" } }
                        .onSuccess { daemonLog.i { "Kill switch restored successfully" } }
                } else {
                    daemonLog.w { "Kill switch restore requested but no config cached — skipping" }
                }
            } else {
                daemonLog.d { "Kill switch restore disabled in settings — skipping" }
            }
        }
        startUdsServer()
        daemonLog.i { "Binding Unix socket at: $socketPath" }
        shutdownLatch.await()
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
                            daemonLog.e(cause) { "Unhandled exception in daemon" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to cause.message),
                            )
                        }
                    }
                    install(hmacShieldPlugin)
                    routing {
                        daemonRoutes(cacheRepository, autoTunnelSupervisor, daemonLogService)
                        tunnelRoutes(backend, autoTunnelSupervisor)
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

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        daemonLog.i { "Daemon stop initiated - closing all tunnels" }
        autoTunnelSupervisor.stop()
        runBlocking {
            backend.stopAllActiveTunnels()
            backend.disableKillSwitch()
        }
        networkMonitor?.stop()
        daemonLog.i { "All tunnels closed - stopping server" }
        server?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        shutdownLatch.countDown()
        daemonLog.i { "UDS server fully stopped" }
    }

    private companion object {
        fun osName(): String = System.getProperty("os.name").orEmpty()

        fun isWindows(): Boolean = osName().startsWith("Windows", ignoreCase = true)
    }
}

package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.plugin.hmacShieldPlugin
import com.zaneschepke.wireguardautotunnel.daemon.routes.backendRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.daemonRoutes
import com.zaneschepke.wireguardautotunnel.daemon.routes.tunnelRoutes
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SystemUtils

class TunnelDaemon(
    private val json: Json,
    private val backend: Backend,
    private val cacheRepository: DaemonCacheRepository,
    private val socketPath: String,
) {
    private var server: EmbeddedServer<*, *>? = null
    private val running = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)
    private val scope = CoroutineScope(Dispatchers.IO)

    // run the daemon
    internal fun run() {
        Logger.i {
            "Daemon starting on ${SystemUtils.OS_NAME} — enabling kill-switch first for leak protection"
        }

        // try to start the kill switch as early as possible to prevent leaks, blocking
        runBlocking {
            val restoreKillSwitch = cacheRepository.getKillSwitchRestore()
            if (restoreKillSwitch) {
                Logger.i { "Restoring kill switch from previous state" }
                backend
                    .setKillSwitch(true)
                    .onFailure { Logger.e(it) { "Failed to restore kill switch" } }
                    .onSuccess { Logger.i { "Kill switch restored successfully" } }

                val bypassLan = cacheRepository.getKillSwitchBypassLan()
                if (bypassLan) {
                    backend
                        .setKillSwitchLanBypass(true)
                        .onFailure { Logger.e(it) { "Failed to restore LAN bypass" } }
                        .onSuccess { Logger.i { "Kill switch LAN bypass restored" } }
                }
            } else {
                Logger.i { "Kill switch restore disabled in settings — skipping" }
            }
        }
        startUdsServer()
        shutdownLatch.await() // block main thread until stop()
    }

    fun startUdsServer() {
        if (!running.compareAndSet(false, true)) return

        Logger.i { "Starting IPC server" }

        val socketFile = File(socketPath)
        val runtimeDir = socketFile.parentFile
        runtimeDir.mkdirs()

        when {
            SystemUtils.IS_OS_WINDOWS ->
                PermissionsHelper.setupDirectoryPermissionsWindows(runtimeDir.absolutePath)
            SystemUtils.IS_OS_UNIX ->
                PermissionsHelper.setupDirectoryPermissionsUnix(runtimeDir.absolutePath)
        }

        socketFile.delete() // delete old socket if exists

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

                        // catch all
                        exception<Throwable> { call, cause ->
                            Logger.e(cause) { "Unhandled exception in daemon" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to cause.message),
                            )
                        }
                    }
                    install(hmacShieldPlugin)
                    routing {
                        daemonRoutes(cacheRepository)
                        tunnelRoutes(backend, cacheRepository)
                        backendRoutes(backend)
                    }
                    monitor.subscribe(ApplicationStarted) {
                        Logger.i { "IPC server started successfully" }
                    }
                }
                .start(wait = false)

        scope.launch {
            when {
                SystemUtils.IS_OS_UNIX ->
                    PermissionsHelper.setupSocketPermissionsWithPollUnix(socketPath)
                SystemUtils.IS_OS_WINDOWS ->
                    PermissionsHelper.setupSocketPermissionsWithPollWindows(socketPath)
            }
        }

        scope.launch {
            val restoreTun = cacheRepository.getRestoreTunnelOnBoot()
            if (restoreTun) {
                Logger.i { "Attempting to restore previous tunnel" }
                val config = cacheRepository.getLastActiveTunnelConfig() ?: return@launch
                val name = cacheRepository.getLastActiveTunnelName() ?: return@launch
                val id = cacheRepository.getLastActiveTunnelId() ?: return@launch
                val tunnel = RunningTunnel(id, name)
                backend.start(tunnel, config)
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        Logger.i { "Daemon stop initiated - closing all tunnels" }
        backend.shutdown()
        Logger.i { "All tunnels closed - stopping server" }
        server?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        shutdownLatch.countDown()
        Logger.i { "UDS server fully stopped" }
    }
}

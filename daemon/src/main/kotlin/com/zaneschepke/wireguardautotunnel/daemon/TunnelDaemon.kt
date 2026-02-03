package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.plugin.UDSPlugins
import com.zaneschepke.wireguardautotunnel.daemon.routes.tunnelCommandRoutes
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class TunnelDaemon(private val json: Json,
                   private val backend: Backend,
                   private val cacheRepository: DaemonCacheRepository,
                   private val socketPath: String
) {
    private var server: EmbeddedServer<*, *>? = null
    private val running = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)
    private val scope = CoroutineScope(Dispatchers.IO)

    // run the daemon
    internal fun run() {
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
            SystemUtils.IS_OS_WINDOWS -> PermissionsHelper.setupDirectoryPermissionsWindows(runtimeDir.absolutePath)
            SystemUtils.IS_OS_UNIX -> PermissionsHelper.setupDirectoryPermissionsUnix(runtimeDir.absolutePath)
        }

        socketFile.delete()  // delete old socket if exists


        server = embeddedServer(CIO, configure = {
            unixConnector(socketPath)
        }) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
            routing {
                get("/status") { call.response.status(HttpStatusCode.OK) }
                route("/tunnel") {
                    install(UDSPlugins.hmacShieldPlugin)
                    tunnelCommandRoutes(json, backend)
                }
            }
            monitor.subscribe(ApplicationStarted) {
                Logger.i { "IPC server started successfully" }
            }
        }.start(wait = false)

        scope.launch {
            when {
                SystemUtils.IS_OS_UNIX -> PermissionsHelper.setupSocketPermissionsWithPollUnix(socketPath)
                SystemUtils.IS_OS_WINDOWS -> PermissionsHelper.setupSocketPermissionsWithPollWindows(socketPath)
            }
        }

        scope.launch {
            // TODO handle startup with cached settings
            val settings = cacheRepository.getKillSwitchSettings()
            val startConfigs = cacheRepository.getStartConfigs()
            Logger.d { "Got kill switch settings $settings" }
            Logger.d { "Got start configs of size ${startConfigs.size}" }
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
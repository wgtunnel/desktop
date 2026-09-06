package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import com.wgtunnel.backend.TunnelBackend
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.SettingsDaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.log.DaemonLogService
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DaemonApplicationProvider
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DesktopNetworkMonitor
import com.zaneschepke.wireguardautotunnel.daemon.util.initLogger
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val log = Logger.withTag("Daemon")

fun main() {
    Logger.setLogWriters(CommonWriter())
    AppVariant.applyProcessDefaults()
    initLogger()
    log.i { "Daemon variant=${AppVariant.current.id} packaged=${AppVariant.isPackaged()}" }

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val networkMonitor = DesktopNetworkMonitor(scope)
    val backend = TunnelBackend(scope, DaemonApplicationProvider(), networkMonitor)
    val cacheRepository = SettingsDaemonCacheRepository(json)
    val daemonLogService = DaemonLogService(cacheRepository)
    val autoTunnelSupervisor =
        AutoTunnelSupervisor(
            backend = backend,
            cacheRepository = cacheRepository,
            networkMonitor = networkMonitor,
            scope = scope,
        )
    val daemon =
        TunnelDaemon(
            json = json,
            backend = backend,
            cacheRepository = cacheRepository,
            socketPath = FilePathsHelper.getDaemonSocketPath(),
            autoTunnelSupervisor = autoTunnelSupervisor,
            networkMonitor = networkMonitor,
            daemonLogService = daemonLogService,
            scope = scope,
        )

    try {
        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    log.i { "Stopping daemon..." }
                    daemon.stop()
                }
            )

        runBlocking { daemonLogService.restore() }
        log.i { "Starting daemon..." }
        daemon.run()
    } catch (e: Exception) {
        log.e(e) { "Shutting down..." }
        exitProcess(1)
    }
}

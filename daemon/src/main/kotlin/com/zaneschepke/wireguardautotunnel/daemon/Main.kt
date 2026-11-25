package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import com.wgtunnel.backend.TunnelBackend
import com.wgtunnel.backend.system.FileOwner
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.WindowsSid
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonSessionStore
import com.zaneschepke.wireguardautotunnel.daemon.data.SettingsDaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.firewall.EarlyKillSwitch
import com.zaneschepke.wireguardautotunnel.daemon.firewall.FirewallCommands
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

fun main(args: Array<String>) {
    // Its service, the early boot unit, uninstallers and "Remove block" all act on this install.
    AppVariant.useBuiltIn(BuildConfig.VARIANT)
    Logger.setLogWriters(CommonWriter())
    AppVariant.applyProcessDefaults()
    initLogger()
    log.i { "Daemon variant=${AppVariant.current.id} packaged=${AppVariant.isPackaged()}" }
    WindowsSid.ownerSidLookup = { path -> FileOwner.sidOf(path.toString()) }

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    FirewallCommands.run(args, json)?.let(::exitProcess)
    // Before anything else is set up, so the kill switch is back as soon as possible.
    val cacheRepository = SettingsDaemonCacheRepository(json)
    val earlyKillSwitch = runBlocking { EarlyKillSwitch.apply(cacheRepository) }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val networkMonitor = DesktopNetworkMonitor(scope)
    val backend = TunnelBackend(scope, DaemonApplicationProvider(), networkMonitor)
    val daemonLogService = DaemonLogService(cacheRepository, scope)
    val autoTunnelSupervisor =
        AutoTunnelSupervisor(
            backend = backend,
            networkMonitor = networkMonitor,
            cacheRepository = cacheRepository,
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
            sessionStore = DaemonSessionStore(json),
            scope = scope,
            earlyKillSwitch = earlyKillSwitch,
        )

    try {
        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    log.i { "Stopping daemon..." }
                    daemon.stop()
                },
            )

        runBlocking { daemonLogService.restore() }
        log.i { "Starting daemon..." }
        daemon.run()
    } catch (e: Exception) {
        log.e(e) { "Shutting down..." }
        exitProcess(1)
    }
}

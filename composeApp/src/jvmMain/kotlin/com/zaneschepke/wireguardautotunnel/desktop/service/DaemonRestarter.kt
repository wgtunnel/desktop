package com.zaneschepke.wireguardautotunnel.desktop.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelBackendCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.DaemonInfoDto
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.servicemanagement.AppServiceStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Moves an outdated daemon onto the binary installed by an app update, then brings back the tunnel
 * and kill switch that were up before, so an update doesn't leave the user disconnected.
 */
class DaemonRestarter(
    private val daemonService: DaemonService,
    private val backendService: BackendService,
    private val tunnelCoordinator: TunnelCoordinator,
    private val tunnelBackendCoordinator: TunnelBackendCoordinator,
    private val tunnelRepository: TunnelRepository,
    private val autoTunnelRepository: AutoTunnelSettingsRepository,
) {
    private val log = Logger.withTag("DaemonRestarter")
    private val mutex = Mutex()

    private val _restarting = MutableStateFlow(false)
    val restarting: StateFlow<Boolean> = _restarting.asStateFlow()

    enum class Outcome {
        RESTARTED,
        NEEDS_APPROVAL,
        FAILED,
    }

    /** [info] is the outdated daemon's, or null when none is connected. */
    suspend fun restart(info: DaemonInfoDto?): Outcome = mutex.withLock {
        _restarting.value = true
        try {
            restartLocked(info)
        } finally {
            _restarting.value = false
        }
    }

    private suspend fun restartLocked(info: DaemonInfoDto?): Outcome {
        if (daemonService.remoteInfo.first()?.version == BuildConfig.APP_VERSION) {
            return Outcome.RESTARTED
        }
        val before = backendService.getStatus().getOrNull()
        log.i { "Restarting daemon v${info?.version} onto v${BuildConfig.APP_VERSION}" }

        // Older daemons have no restart route. On macOS the app can still swap them through
        // SMAppService, elsewhere that needs a privileged command.
        val requested =
            if (info?.canRestart == true) {
                daemonService
                    .requestRestart()
                    .onFailure { log.w(it) { "Daemon restart request failed" } }
                    .isSuccess
            } else {
                isMacOS && MacBackgroundServices.reregister(MacBackgroundServices.daemon)
            }
        if (isMacOS) {
            withContext(Dispatchers.IO) {
                MacBackgroundServices.reregister(MacBackgroundServices.wifiAgent)
            }
        }
        if (!requested) return Outcome.FAILED

        val updated =
            withTimeoutOrNull(RESTART_TIMEOUT) {
                daemonService.remoteInfo.first { it?.version == BuildConfig.APP_VERSION }
            }
        if (updated == null) {
            log.w { "Daemon did not come back on v${BuildConfig.APP_VERSION}" }
            val needsApproval =
                isMacOS &&
                    MacBackgroundServices.status(MacBackgroundServices.daemon) ==
                        AppServiceStatus.REQUIRES_APPROVAL
            return if (needsApproval) Outcome.NEEDS_APPROVAL else Outcome.FAILED
        }

        restoreSession(
            tunnelIds = before?.activeTunnels?.map { it.id }.orEmpty(),
            killSwitch = before?.killSwitchEnabled == true,
        )
        return Outcome.RESTARTED
    }

    private suspend fun restoreSession(tunnelIds: List<Long>, killSwitch: Boolean) {
        val now = backendService.getStatus().getOrNull()
        if (killSwitch && now?.killSwitchEnabled != true) {
            tunnelBackendCoordinator.applyKillSwitchConfig().onFailure {
                log.e(it) { "Failed to restore the kill switch after the daemon restart" }
            }
        }
        // Auto-tunnel picks its own tunnel once its plan reaches the new daemon.
        if (tunnelIds.isEmpty() || autoTunnelRepository.get().isAutoTunnelEnabled) return
        val running = now?.activeTunnels?.map { it.id }.orEmpty()
        tunnelIds
            .filterNot { it in running }
            .mapNotNull { tunnelRepository.getById(it) }
            .forEach { config ->
                log.i { "Restoring tunnel ${config.name} after the daemon restart" }
                tunnelCoordinator.startTunnel(config).onFailure {
                    log.e(it) { "Failed to restore tunnel ${config.name}" }
                }
            }
    }

    private companion object {
        // WinSW waits 10s before relaunching, launchd can throttle for about as long.
        val RESTART_TIMEOUT = 30.seconds
        val isMacOS = Platform.Current == Platform.MacOS
    }
}

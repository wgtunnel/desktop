package com.zaneschepke.wireguardautotunnel.client.orchestration

import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService

class TunnelBackendCoordinator(
    private val tunnelCoordinator: TunnelCoordinator,
    private val backendService: BackendService,
    private val settingsRepository: GeneralSettingRepository,
    private val lockdownRepository: LockdownSettingsRepository,
    private val tunnelRepository: TunnelRepository,
) {

    suspend fun changeMode(newMode: TunnelMode): Result<Unit> {
        val settings = settingsRepository.get()
        val oldMode = settings.selectableTunnelMode
        if (oldMode == newMode) return Result.success(Unit)

        return runCatching {
            tunnelCoordinator.stopActiveTunnels().getOrThrow()
            settingsRepository.updateTunnelMode(newMode)
        }
    }

    suspend fun setKillSwitchEnabled(enabled: Boolean): Result<Unit> {
        val lockdown = lockdownRepository.get()
        val mode = settingsRepository.get().selectableTunnelMode
        return runCatching {
            lockdownRepository.updateEnabled(enabled)
            if (enabled) {
                backendService
                    .setKillSwitch(true, killSwitchDto(lockdown.copy(enabled = true)))
                    .getOrThrow()
            } else {
                backendService.setKillSwitch(false).getOrThrow()
            }
            if (mode == TunnelMode.PROXY) {
                tunnelCoordinator.restartActiveTunnels().getOrThrow()
            }
        }
    }

    suspend fun applyKillSwitchConfig(): Result<Unit> {
        val lockdown = lockdownRepository.get()
        if (!lockdown.enabled) return Result.success(Unit)
        return backendService.setKillSwitch(true, killSwitchDto(lockdown))
    }

    private suspend fun killSwitchDto(
        lockdown: com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings
    ) = tunnelCoordinator.killSwitchDto(lockdown, activeRunConfig())

    private suspend fun activeRunConfig() =
        tunnelCoordinator.activeTunnelIds().firstOrNull()?.let {
            tunnelRepository.getById(it)?.asConfig()
        }
}

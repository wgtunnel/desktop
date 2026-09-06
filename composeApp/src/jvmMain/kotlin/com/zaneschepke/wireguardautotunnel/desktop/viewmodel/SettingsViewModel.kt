package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelBackendCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SettingsUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.asUserMessage
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class SettingsViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val lockdownRepository: LockdownSettingsRepository,
    private val monitoringRepository: MonitoringSettingsRepository,
    private val dnsSettingsRepository: DnsSettingsRepository,
    private val tunnelRepository: TunnelRepository,
    private val daemonService: DaemonService,
    private val tunnelBackendCoordinator: TunnelBackendCoordinator,
) : OrbitContainerHost<SettingsUiState, SettingsUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<SettingsUiState, AppSideEffect>(
            SettingsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent {
                combine(
                        settingsRepository.flow,
                        lockdownRepository.flow,
                        monitoringRepository.flow,
                        dnsSettingsRepository.flow,
                        tunnelRepository.globalTunnelFlow,
                    ) { settings, lockdown, monitoring, dns, global ->
                        SettingsSnapshot(settings, lockdown, monitoring, dns, global)
                    }
                    .collect { snapshot ->
                        reduce {
                            state.copy(
                                isLoaded = true,
                                settings = snapshot.settings,
                                lockdown = snapshot.lockdown,
                                monitoring = snapshot.monitoring,
                                dns = snapshot.dns,
                                globalTunnelConfig = snapshot.global,
                            )
                        }
                    }
            }
        }

    fun onRestoreTunnelOnBoot(enabled: Boolean) = intent {
        daemonService.setRestoreTunnel(enabled).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onSeamlessRecovery(enabled: Boolean) = intent {
        settingsRepository.updateSeamlessRecovery(enabled)
    }

    fun onSeamlessRecoveryBounceDelay(seconds: Int) = intent {
        settingsRepository.updateSeamlessRecoveryBounceDelay(seconds)
    }

    fun onTunnelMode(mode: TunnelMode) = intent {
        tunnelBackendCoordinator.changeMode(mode).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onKillSwitchEnabled(enabled: Boolean) = intent {
        tunnelBackendCoordinator.setKillSwitchEnabled(enabled).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onBypassLan(enabled: Boolean) = intent {
        lockdownRepository.updateBypassLan(enabled)
        tunnelBackendCoordinator.applyKillSwitchConfig().onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onRestoreKillSwitchOnBoot(enabled: Boolean) = intent {
        daemonService.setRestoreKillSwitch(enabled).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun setGlobalAmneziaEnabled(enabled: Boolean) = intent {
        settingsRepository.updateGlobalAmneziaEnabled(enabled)
    }

    fun setGlobalTunnelDnsEnabled(enabled: Boolean) = intent {
        dnsSettingsRepository.upsert(state.dns.copy(isGlobalTunnelConfigDnsEnabled = enabled))
    }

    fun setLocalLogging(enabled: Boolean) = intent {
        monitoringRepository.upsert(state.monitoring.copy(isLocalLogsEnabled = enabled))
    }

    private data class SettingsSnapshot(
        val settings: com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings,
        val lockdown: com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings,
        val monitoring: com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings,
        val dns: com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings,
        val global: com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig?,
    )
}

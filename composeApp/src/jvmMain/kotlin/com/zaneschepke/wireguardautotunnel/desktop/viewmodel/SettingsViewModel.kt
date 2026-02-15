package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.LockdownIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SettingsUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.asUserMessage
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class SettingsViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val lockdownRepository: LockdownSettingsRepository,
    private val backendService: BackendService,
    private val daemonService: DaemonService,
) : ContainerHost<SettingsUiState, AppSideEffect>, ViewModel() {

    override val container =
        container<SettingsUiState, AppSideEffect>(
            SettingsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent {
                combine(settingsRepository.flow, lockdownRepository.flow) { settings, lockdown ->
                        Pair(lockdown, settings)
                    }
                    .collect { (lockdown, settings) ->
                        reduce {
                            state.copy(
                                isLoaded = true,
                                lockdownEnabled = lockdown.enabled,
                                lockdownRestoreOnBootEnabled = lockdown.restoreOnBoot,
                                lockdownBypassEnabled = lockdown.bypassLan,
                                tunnelRestoreOnBootEnabled = settings.restoreTunnelOnBoot,
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

    fun onLockdownAction(intent: LockdownIntent) = intent {
        when (intent) {
            is LockdownIntent.ToggleBypassLan -> {
                backendService.setKillSwitchLanBypass(intent.enabled)
            }
            is LockdownIntent.ToggleMaster -> {
                backendService.setKillSwitch(intent.enabled)
            }
            is LockdownIntent.TogglePersist -> {
                daemonService.setRestoreKillSwitch(intent.enabled)
            }
        }.onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }
}

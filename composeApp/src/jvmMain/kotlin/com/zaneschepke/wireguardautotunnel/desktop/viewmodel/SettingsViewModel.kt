package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendCommandService
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.LockdownIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SettingsUiState
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class SettingsViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val lockdownRepository: LockdownSettingsRepository,
    private val backendCommandService: BackendCommandService,
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
                            )
                        }
                    }
            }
        }

    fun onLockdownAction(intent: LockdownIntent) = intent {
        when (intent) {
            is LockdownIntent.ToggleBypassLan -> {}
            is LockdownIntent.ToggleMaster -> {
                backendCommandService.setKillSwitch(intent.enabled)
            }
            is LockdownIntent.TogglePersist -> {}
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AppViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val daemonService: DaemonService,
    private val backendService: BackendService,
) : ContainerHost<AppUiState, AppSideEffect>, ViewModel() {

    override val container =
        container<AppUiState, AppSideEffect>(
            AppUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent {
                settingsRepository.flow.collect { settings ->
                    if (!state.isLoaded || settings.locale != state.locale) {
                        LocaleUpdater.updateLocale(state.locale)
                    }
                    reduce {
                        state.copy(
                            isLoaded = true,
                            theme = settings.theme,
                            locale = settings.locale ?: state.locale,
                            alreadyDonated = settings.alreadyDonated,
                        )
                    }
                }
            }
            intent { daemonService.alive.collect { reduce { state.copy(daemonConnected = it) } } }
            intent {
                backendService.statusFlow().collect {
                    reduce { state.copy(lockdownActive = it.killSwitchEnabled) }
                }
            }
        }

    fun setAlreadyDonated(donated: Boolean) = intent {
        settingsRepository.updateAlreadyDonated(donated)
    }

    fun setTheme(theme: Theme) = intent { settingsRepository.updateTheme(theme) }
}

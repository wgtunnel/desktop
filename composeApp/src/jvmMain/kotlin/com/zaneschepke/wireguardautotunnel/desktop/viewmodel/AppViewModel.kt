package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.AutoTunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_available_in_support_template
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import dev.nucleusframework.updater.UpdateResult
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class AppViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val tunnelRepository: TunnelRepository,
    private val daemonService: DaemonService,
    private val backendService: BackendService,
    private val appUpdater: AppUpdater,
    @Suppress("unused") private val autoTunnelCoordinator: AutoTunnelCoordinator,
    @Suppress("unused") private val logCoordinator: LogCoordinator,
) : OrbitContainerHost<AppUiState, AppUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<AppUiState, AppSideEffect>(
            AppUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent { tunnelRepository.ensureGlobalConfigExists() }
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
                            useSystemColors = settings.useSystemColors,
                        )
                    }
                }
            }
            intent { daemonService.alive.collect { reduce { state.copy(daemonConnected = it) } } }
            intent {
                backendService
                    .statusFlow()
                    .map { it.killSwitchEnabled to it.activeTunnels }
                    .distinctUntilChanged()
                    .collect { (lockdown, tunnelStates) ->
                        reduce {
                            state.copy(tunnelStatuses = tunnelStates, lockdownActive = lockdown)
                        }
                    }
            }
            intent {
                if (!appUpdater.isSupported()) return@intent
                when (val result = appUpdater.check()) {
                    is UpdateResult.Available ->
                        postSideEffect(
                            AppSideEffect.Toast(
                                getString(
                                    Res.string.update_available_in_support_template,
                                    result.info.version,
                                ),
                                ToastType.Info,
                            )
                        )
                    else -> Unit
                }
            }
        }

    fun setAlreadyDonated(donated: Boolean) = intent {
        settingsRepository.updateAlreadyDonated(donated)
    }

    fun setUseSystemColors(enabled: Boolean) = intent {
        settingsRepository.updateSystemColors(enabled)
    }

    fun setTheme(theme: Theme) = intent { settingsRepository.updateTheme(theme) }
}

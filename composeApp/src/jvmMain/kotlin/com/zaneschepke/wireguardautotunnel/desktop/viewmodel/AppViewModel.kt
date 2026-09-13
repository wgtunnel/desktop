package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.AutoTunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_not_running
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_outdated_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_restart_command_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_start_command_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_available_in_support_template
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.DaemonConnectionStatus
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import dev.nucleusframework.updater.UpdateResult
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

@OptIn(FlowPreview::class)
class AppViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val autoTunnelRepository: AutoTunnelSettingsRepository,
    private val tunnelRepository: TunnelRepository,
    private val daemonService: DaemonService,
    private val backendService: BackendService,
    private val appUpdater: AppUpdater,
    @Suppress("unused") private val autoTunnelCoordinator: AutoTunnelCoordinator,
    @Suppress("unused") private val logCoordinator: LogCoordinator,
) : OrbitContainerHost<AppUiState, AppUiState, AppSideEffect>, ViewModel() {

    private val log = Logger.withTag("AppViewModel")

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
                            tunnelMode = settings.tunnelMode,
                        )
                    }
                }
            }
            intent {
                daemonService.alive.collect { alive ->
                    reduce {
                        state.copy(
                            daemonStatus =
                                if (alive) DaemonConnectionStatus.SYNCING
                                else DaemonConnectionStatus.DISCONNECTED
                        )
                    }
                }
            }
            intent {
                daemonService.alive
                    .debounce { alive -> if (alive) 0.seconds else DISCONNECT_GRACE_PERIOD }
                    .distinctUntilChanged()
                    .collect { alive ->
                        if (alive) {
                            postSideEffect(AppSideEffect.DismissToast(DAEMON_NOT_RUNNING_TOAST_ID))
                        } else {
                            postSideEffect(
                                AppSideEffect.ActionableToast(
                                    id = DAEMON_NOT_RUNNING_TOAST_ID,
                                    message = getString(Res.string.daemon_not_running),
                                    copyText = daemonStartCommand(),
                                    copyLabel = getString(Res.string.daemon_start_command_label),
                                )
                            )
                        }
                    }
            }
            intent {
                daemonService.remoteVersion.filterNotNull().collect { remoteVersion ->
                    if (remoteVersion != BuildConfig.APP_VERSION) {
                        postSideEffect(
                            AppSideEffect.ActionableToast(
                                id = DAEMON_OUTDATED_TOAST_ID,
                                message =
                                    getString(
                                        Res.string.daemon_outdated_template,
                                        remoteVersion,
                                        BuildConfig.APP_VERSION,
                                    ),
                                copyText = daemonRestartCommand(),
                                copyLabel = getString(Res.string.daemon_restart_command_label),
                            )
                        )
                    } else {
                        postSideEffect(AppSideEffect.DismissToast(DAEMON_OUTDATED_TOAST_ID))
                    }
                }
            }
            intent {
                autoTunnelRepository.flow.collect { settings ->
                    reduce { state.copy(autoTunnelEnabled = settings.isAutoTunnelEnabled) }
                }
            }
            intent {
                backendService
                    .statusFlow()
                    .map { it.killSwitchEnabled to it.activeTunnels }
                    .distinctUntilChanged()
                    .collect { (lockdown, tunnelStates) ->
                        reduce {
                            state.copy(
                                tunnelStatuses = tunnelStates,
                                lockdownActive = lockdown,
                                daemonStatus = DaemonConnectionStatus.CONNECTED,
                            )
                        }
                    }
            }
            intent {
                if (!appUpdater.isSupported()) return@intent
                when (val result = appUpdater.check()) {
                    is UpdateResult.Available -> {
                        val alreadyNotified =
                            settingsRepository.get().lastNotifiedUpdateVersion ==
                                result.info.version
                        if (!alreadyNotified) {
                            // Recorded before showing, not after any install so a fresh, newer
                            // release always overwrites this and notifies again regardless of
                            // whether the user ever acted on the last one.
                            settingsRepository.updateLastNotifiedUpdateVersion(result.info.version)
                            postSideEffect(
                                AppSideEffect.UpdateAvailableToast(
                                    id = UPDATE_AVAILABLE_TOAST_ID,
                                    message =
                                        getString(
                                            Res.string.update_available_in_support_template,
                                            result.info.version,
                                        ),
                                )
                            )
                        }
                    }
                    is UpdateResult.Error -> log.w(result.exception) { "Update check failed" }
                    UpdateResult.NotAvailable -> Unit
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

    private fun daemonStartCommand(): String =
        if (isWindows) "net start $daemonServiceName"
        else "sudo systemctl enable --now $daemonServiceName.service"

    private fun daemonRestartCommand(): String =
        if (isWindows) "net stop $daemonServiceName && net start $daemonServiceName"
        else "sudo systemctl restart $daemonServiceName.service"

    companion object {
        private const val DAEMON_NOT_RUNNING_TOAST_ID = "daemon_not_running"
        private const val DAEMON_OUTDATED_TOAST_ID = "daemon_outdated"
        private const val UPDATE_AVAILABLE_TOAST_ID = "update_available"
        private val DISCONNECT_GRACE_PERIOD = 5.seconds

        private val isWindows =
            System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        private val daemonServiceName = "${AppVariant.current.linuxFsName}-daemon"
    }
}

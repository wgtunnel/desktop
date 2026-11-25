package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.dokar.sonner.ToastType
import com.materialkolor.ktx.themeColor
import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.data.model.TrayIconAppearance
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ClientCacheRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.AutoTunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.DnsSettingsCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.KillSwitchSync
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_outdated_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.background_service_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_down_kill_switch_blocking
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_not_running
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_outdated_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_restart_action
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_restart_command_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_restarted
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_restarting
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_start_command_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donate_action
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donation_prompt_message
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kill_switch_block_remove_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kill_switch_block_removed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.mac_service_requires_approval_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.open_system_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.remove_block_action
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restart_app_action
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.theme_from_image_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wifi_helper_service_name
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.DaemonInfoDto
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.desktop.service.DaemonRestarter
import com.zaneschepke.wireguardautotunnel.desktop.service.KillSwitchUnblocker
import com.zaneschepke.wireguardautotunnel.desktop.service.MacBackgroundServices
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.DaemonConnectionStatus
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.Aqua
import com.zaneschepke.wireguardautotunnel.desktop.update.AppRelauncher
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import com.zaneschepke.wireguardautotunnel.desktop.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.desktop.wifi.MacWifiNameCoordinator
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.servicemanagement.AppService
import dev.nucleusframework.servicemanagement.AppServiceManager
import dev.nucleusframework.servicemanagement.AppServiceStatus
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.Version
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer

@OptIn(FlowPreview::class)
class AppViewModel(
    private val settingsRepository: GeneralSettingRepository,
    private val clientCacheRepository: ClientCacheRepository,
    private val autoTunnelRepository: AutoTunnelSettingsRepository,
    private val tunnelRepository: TunnelRepository,
    private val daemonService: DaemonService,
    private val backendService: BackendService,
    private val appUpdater: AppUpdater,
    private val daemonRestarter: DaemonRestarter,
    private val lockdownRepository: LockdownSettingsRepository,
    private val killSwitchSync: KillSwitchSync,
    @Suppress("unused") private val autoTunnelCoordinator: AutoTunnelCoordinator,
    @Suppress("unused") private val logCoordinator: LogCoordinator,
    @Suppress("unused") private val dnsSettingsCoordinator: DnsSettingsCoordinator,
    @Suppress("unused") private val macWifiNameCoordinator: MacWifiNameCoordinator,
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
                        LocaleUpdater.updateLocale(settings.locale ?: LocaleUtil.systemLocaleTag)
                    }
                    reduce {
                        state.copy(
                            isLoaded = true,
                            theme = settings.theme,
                            locale = settings.locale,
                            alreadyDonated = settings.alreadyDonated,
                            restoreTunnelOnBoot = settings.restoreTunnelOnBoot,
                            launchAtLogin = settings.launchAtLogin,
                            useSystemColors = settings.useSystemColors,
                            customSeedColor = settings.customSeedColor,
                            accentStyle = settings.accentStyle,
                            trayIconAppearance = settings.trayIconAppearance,
                            tunnelMode = settings.tunnelMode,
                        )
                    }
                }
            }
            intent {
                combine(daemonService.alive, backendService.statusFlow()) { alive, status ->
                        alive to status
                    }
                    .collect { (alive, status) ->
                        reduce {
                            if (!alive) {
                                state.copy(
                                    daemonStatus = DaemonConnectionStatus.DISCONNECTED,
                                    lockdownActive = false,
                                    tunnelStatuses = emptyList(),
                                )
                            } else {
                                state.copy(
                                    daemonStatus = DaemonConnectionStatus.CONNECTED,
                                    tunnelStatuses = status.activeTunnels,
                                    lockdownActive = status.killSwitchEnabled,
                                )
                            }
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
                            // Older daemons reports the kill switch off until the restarter
                            // puts it back.
                            if (!daemonRestarter.restarting.value) {
                                killSwitchSync.syncFromDaemon().onFailure {
                                    log.w(it) { "Kill switch sync failed" }
                                }
                            }
                        } else if (daemonRestarter.restarting.value) {
                            // A Windows restart takes up to 15s, past the grace period, and has
                            // its own toast.
                        } else if (lockdownRepository.get().enabled) {
                            // The daemon leaves the kill switch enforced when it stops.
                            postSideEffect(blockedWhileDaemonDown())
                        } else {
                            val message = getString(Res.string.daemon_not_running)
                            postSideEffect(
                                if (isWindows || isSystemdActive) {
                                    AppSideEffect.ActionableToast(
                                        id = DAEMON_NOT_RUNNING_TOAST_ID,
                                        message = message,
                                        copyText = daemonStartCommand(),
                                        copyLabel =
                                            getString(Res.string.daemon_start_command_label),
                                    )
                                } else {
                                    AppSideEffect.Toast(message = message, type = ToastType.Warning)
                                },
                            )
                        }
                    }
            }
            intent {
                daemonService.remoteInfo.filterNotNull().collect { info ->
                    if (info.version == BuildConfig.APP_VERSION) {
                        postSideEffect(AppSideEffect.DismissToast(DAEMON_OUTDATED_TOAST_ID))
                        return@collect
                    }
                    // A package manager upgrade can restart the daemon on the new version while
                    // this older app is still open. Only restarting the app fixes it.
                    if (isNewerThanApp(info.version)) {
                        postSideEffect(
                            AppSideEffect.TextActionToast(
                                id = DAEMON_OUTDATED_TOAST_ID,
                                message = getString(Res.string.app_outdated_template, info.version),
                                actionLabel = getString(Res.string.restart_app_action),
                                onAction = { AppRelauncher.relaunch() },
                                type = ToastType.Info,
                            ),
                        )
                        return@collect
                    }
                    // macOS has no command to hand the user, and the app can restart its own
                    // SMAppService daemon, so it finishes the update without asking.
                    if (isMacOS) restartOutdatedDaemon(info) else offerDaemonRestart(info)
                }
            }
            intent {
                autoTunnelRepository.flow.collect { settings ->
                    reduce { state.copy(autoTunnelEnabled = settings.isAutoTunnelEnabled) }
                }
            }

            // SMAppService requires an explicit register() call before macOS will ever
            // load it. Safe to call on every launch as it no-ops if already registered/approved.
            //
            // AppServiceManager.register() called twice on the same coroutine hangs, so we register
            // the agent and daemon on different coroutines
            if (isMacOS) {
                intent {
                    registerMacService(
                        MacBackgroundServices.daemon,
                        logLabel = "daemon",
                        displayName = getString(Res.string.background_service_name),
                        toastId = DAEMON_APPROVAL_TOAST_ID,
                    )
                }
                intent {
                    withContext(Dispatchers.IO) {
                        registerMacService(
                            MacBackgroundServices.wifiAgent,
                            logLabel = "Wi-Fi helper",
                            displayName = getString(Res.string.wifi_helper_service_name),
                            toastId = WIFI_AGENT_APPROVAL_TOAST_ID,
                        )
                    }
                }
            }

            intent {
                val result = appUpdater.check(silent = true)
                if (result is UpdateResult.Error) log.w(result.exception) { "Update check failed" }
            }

            intent {
                val lastSeenAppVersion = clientCacheRepository.getLastSeenAppVersion()
                if (lastSeenAppVersion != BuildConfig.APP_VERSION) {
                    if (lastSeenAppVersion != null && !settingsRepository.get().alreadyDonated) {
                        postSideEffect(
                            AppSideEffect.TextActionToast(
                                id = DONATION_PROMPT_TOAST_ID,
                                message = getString(Res.string.donation_prompt_message),
                                actionLabel = getString(Res.string.donate_action),
                                onAction = {
                                    intent { postSideEffect(AppSideEffect.Navigate(Route.Donate)) }
                                },
                                type = ToastType.Normal,
                                duration = DONATION_PROMPT_DURATION,
                            ),
                        )
                    }
                    clientCacheRepository.updateLastSeenAppVersion(BuildConfig.APP_VERSION)
                }
            }
        }

    fun setAlreadyDonated(donated: Boolean) = intent {
        settingsRepository.updateAlreadyDonated(donated)
    }

    fun setUseSystemColors(enabled: Boolean) = intent {
        settingsRepository.updateSystemColors(enabled)
        // Mutually exclusive with a picked/derived accent color.
        if (enabled) settingsRepository.updateCustomSeedColor(null)
    }

    fun setTheme(theme: Theme) = intent { settingsRepository.updateTheme(theme) }

    fun setLocale(locale: String?) = intent { settingsRepository.updateLocale(locale) }

    fun setCustomSeedColor(color: Color?) = intent {
        settingsRepository.updateCustomSeedColor(color?.toArgb())
        // Mutually exclusive with the system accent color.
        if (color != null) settingsRepository.updateSystemColors(false)
    }

    fun setSeedColorFromImage(image: ImageBitmap) = intent {
        val color = runCatching { image.themeColor(fallback = Aqua) }.getOrNull()
        if (color == null) {
            postSideEffect(
                AppSideEffect.Toast(
                    message = getString(Res.string.theme_from_image_failed),
                    type = ToastType.Error,
                ),
            )
            return@intent
        }
        settingsRepository.updateCustomSeedColor(color.toArgb())
        settingsRepository.updateSystemColors(false)
    }

    fun setAccentStyle(style: AccentStyle) = intent { settingsRepository.updateAccentStyle(style) }

    fun setTrayIconAppearance(appearance: TrayIconAppearance) = intent {
        settingsRepository.updateTrayIconAppearance(appearance)
    }

    // Only called when isWindows or isSystemdActive.
    private fun daemonStartCommand(): String =
        if (isWindows) "net start $daemonServiceName"
        else "sudo systemctl enable --now $daemonServiceName.service"

    private fun daemonRestartCommand(): String =
        if (isWindows) "net stop $daemonServiceName && net start $daemonServiceName"
        else "sudo systemctl restart $daemonServiceName.service"

    private suspend fun Syntax<AppUiState, AppSideEffect>.registerMacService(
        service: AppService,
        logLabel: String,
        displayName: String,
        toastId: String,
    ) {
        AppServiceManager.register(service).onFailure {
            log.w(it) { "Failed to register macOS $logLabel" }
        }
        val status = AppServiceManager.status(service)
        log.i { "macOS $logLabel status: $status" }
        if (status == AppServiceStatus.REQUIRES_APPROVAL) {
            postSideEffect(
                AppSideEffect.NavigableToast(
                    id = toastId,
                    message =
                        getString(Res.string.mac_service_requires_approval_template, displayName),
                    actionLabel = getString(Res.string.open_system_settings),
                    onAction = { AppServiceManager.openSystemSettingsLoginItems() },
                ),
            )
        }
    }

    private suspend fun blockedWhileDaemonDown() =
        AppSideEffect.TextActionToast(
            id = DAEMON_NOT_RUNNING_TOAST_ID,
            message = getString(Res.string.daemon_down_kill_switch_blocking),
            actionLabel = getString(Res.string.remove_block_action),
            onAction = { intent { removeKillSwitchBlock() } },
            type = ToastType.Warning,
        )

    private suspend fun Syntax<AppUiState, AppSideEffect>.removeKillSwitchBlock() {
        when (KillSwitchUnblocker.removeBlock()) {
            KillSwitchUnblocker.Outcome.REMOVED -> {
                // The purge also turned it off in the daemon, so match that here.
                lockdownRepository.updateEnabled(false)
                postSideEffect(
                    AppSideEffect.Toast(
                        message = getString(Res.string.kill_switch_block_removed),
                        type = ToastType.Success,
                        id = DAEMON_NOT_RUNNING_TOAST_ID,
                        duration = DAEMON_RESTARTED_TOAST_DURATION,
                    ),
                )
            }
            KillSwitchUnblocker.Outcome.CANCELLED -> postSideEffect(blockedWhileDaemonDown())
            KillSwitchUnblocker.Outcome.FAILED ->
                postSideEffect(
                    AppSideEffect.Toast(
                        message = getString(Res.string.kill_switch_block_remove_failed),
                        type = ToastType.Error,
                    ),
                )
        }
    }

    // Unparseable versions count as older, which keeps the daemon restart path.
    private fun isNewerThanApp(daemonVersion: String): Boolean = runCatching {
        Version.fromString(daemonVersion) > Version.fromString(BuildConfig.APP_VERSION)
    }
        .getOrDefault(false)

    private suspend fun Syntax<AppUiState, AppSideEffect>.restartOutdatedDaemon(
        info: DaemonInfoDto?,
    ) {
        postSideEffect(
            AppSideEffect.Toast(
                message = getString(Res.string.daemon_restarting),
                type = ToastType.Info,
                id = DAEMON_OUTDATED_TOAST_ID,
            ),
        )
        when (daemonRestarter.restart(info)) {
            DaemonRestarter.Outcome.RESTARTED ->
                postSideEffect(
                    AppSideEffect.Toast(
                        message = getString(Res.string.daemon_restarted, BuildConfig.APP_VERSION),
                        type = ToastType.Success,
                        id = DAEMON_OUTDATED_TOAST_ID,
                        duration = DAEMON_RESTARTED_TOAST_DURATION,
                    ),
                )
            DaemonRestarter.Outcome.NEEDS_APPROVAL ->
                postSideEffect(
                    AppSideEffect.NavigableToast(
                        id = DAEMON_OUTDATED_TOAST_ID,
                        message =
                            getString(
                                Res.string.mac_service_requires_approval_template,
                                getString(Res.string.background_service_name),
                            ),
                        actionLabel = getString(Res.string.open_system_settings),
                        onAction = { AppServiceManager.openSystemSettingsLoginItems() },
                    ),
                )
            DaemonRestarter.Outcome.FAILED -> offerDaemonRestart(info)
        }
    }

    // A restart the user starts, or a copied command when the daemon can't restart itself.
    private suspend fun Syntax<AppUiState, AppSideEffect>.offerDaemonRestart(info: DaemonInfoDto?) {
        val message =
            getString(
                Res.string.daemon_outdated_template,
                info?.version ?: "?",
                BuildConfig.APP_VERSION,
            )
        postSideEffect(
            if (isMacOS || info?.canRestart == true) {
                AppSideEffect.TextActionToast(
                    id = DAEMON_OUTDATED_TOAST_ID,
                    message = message,
                    actionLabel = getString(Res.string.daemon_restart_action),
                    onAction = { intent { restartOutdatedDaemon(info) } },
                    type = ToastType.Warning,
                )
            } else if (isWindows || isSystemdActive) {
                AppSideEffect.ActionableToast(
                    id = DAEMON_OUTDATED_TOAST_ID,
                    message = message,
                    copyText = daemonRestartCommand(),
                    copyLabel = getString(Res.string.daemon_restart_command_label),
                )
            } else {
                AppSideEffect.Toast(message = message, type = ToastType.Warning)
            },
        )
    }

    companion object {
        private const val DAEMON_NOT_RUNNING_TOAST_ID = "daemon_not_running"
        private const val DAEMON_OUTDATED_TOAST_ID = "daemon_outdated"
        private const val DAEMON_APPROVAL_TOAST_ID = "daemon_requires_approval"
        private const val WIFI_AGENT_APPROVAL_TOAST_ID = "wifi_agent_requires_approval"
        private const val DONATION_PROMPT_TOAST_ID = "donation_prompt"
        private val DISCONNECT_GRACE_PERIOD = 5.seconds
        private val DONATION_PROMPT_DURATION = 30.seconds
        private val DAEMON_RESTARTED_TOAST_DURATION = 4.seconds

        private val isWindows = Platform.Current == Platform.Windows
        private val isMacOS = Platform.Current == Platform.MacOS
        private val daemonServiceName = "${AppVariant.current.linuxFsName}-daemon"
        private val isSystemdActive = File("/run/systemd/system").isDirectory
    }
}

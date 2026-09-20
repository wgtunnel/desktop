package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallDesktop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.rememberToasterState
import com.wgtunnel.backend.BackendLog
import com.wgtunnel.backend.LogLevel
import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.di.databaseModule
import com.zaneschepke.wireguardautotunnel.client.di.serviceModule
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ClientCacheRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.appicon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.autolaunch_update_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dismiss
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.lockdown_active
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.titleicon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tray_exit
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tray_minimize_to_tray
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tray_open
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.desktop.di.viewModelModule
import com.zaneschepke.wireguardautotunnel.desktop.ui.WindowIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.CommandToastMessage
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.CopyCommandAction
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.NavigateAction
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asColor
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asTooltipMessage
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TrayBadgeState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.HealthyGreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WGTunnelTheme
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WarningAmber
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.NucleusWindow
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.autolaunch.AutoLaunchConfig
import dev.nucleusframework.autolaunch.AutoLaunchResult
import dev.nucleusframework.composenativetray.tray.api.Tray
import dev.nucleusframework.core.runtime.SingleInstanceManager
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.energymanager.EnergyManager
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.nucleusframework.window.newFullscreenControls
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import org.orbitmvi.orbit.compose.collectAsState

fun main(args: Array<String>) {
    Logger.setLogWriters(CommonWriter())
    AppVariant.applyProcessDefaults()
    Logger.setLogWriters(platformLogWriter())
    Logger.setTag("App")
    BackendLog.setMinLevel(
        if (AppVariant.current == AppVariant.DEBUG) LogLevel.Debug else LogLevel.Info
    )
    Logger.i { "App variant=${AppVariant.current.id} packaged=${AppVariant.isPackaged()}" }

    // Allow variants of the app to run as single instance
    SingleInstanceManager.configuration =
        SingleInstanceManager.Configuration(
            lockFilesDir = Paths.get(FilePathsHelper.getDatabaseDir().path),
            lockIdentifier = AppVariant.current.lockIdentifier,
        )

    AutoLaunchConfig.backgroundReason = "Resume tunnels and auto-tunnel after login"
    AutoLaunch.preload()

    val wasStartedAtLogin = AutoLaunch.wasStartedAtLogin(args)

    nucleusApplication(args = args, backend = NucleusBackend.Tao) {
        var nucleusWindowRef: NucleusWindow? by remember { mutableStateOf(null) }
        // A login-triggered launch should come up quietly in the tray
        var isMainWindowVisible by remember { mutableStateOf(!wasStartedAtLogin) }

        var theme by remember { mutableStateOf(Theme.DEFAULT) }
        var useSystemColors by remember { mutableStateOf(false) }
        var customSeedColor by remember { mutableStateOf<Color?>(null) }
        var accentStyle by remember { mutableStateOf(AccentStyle.TONAL_SPOT) }

        val windowState = rememberWindowState(size = DpSize(1000.dp, 700.dp))

        fun bringToFront() {
            val win = nucleusWindowRef ?: return
            win.setMinimized(false)
            win.show()
            win.toFront()
            win.requestFocus()
        }

        fun handleWindowIntent(intent: WindowIntent) {
            when (intent) {
                WindowIntent.SHOW -> {
                    isMainWindowVisible = true
                    windowState.isMinimized = false
                    bringToFront()
                }

                WindowIntent.HIDE -> {
                    isMainWindowVisible = false
                }

                WindowIntent.TOGGLE -> {
                    if (isMainWindowVisible) {
                        handleWindowIntent(WindowIntent.HIDE)
                    } else {
                        handleWindowIntent(WindowIntent.SHOW)
                    }
                }
            }
        }

        val isSingleInstance =
            SingleInstanceManager.isSingleInstance(
                onRestoreRequest = { handleWindowIntent(WindowIntent.SHOW) }
            )

        if (!isSingleInstance) {
            exitApplication()
            return@nucleusApplication
        }

        var trayBadgeState: TrayBadgeState? by remember { mutableStateOf(null) }
        val appIcon = painterResource(Res.drawable.appicon)
        val appName = stringResource(Res.string.app_name)
        val trayOpenLabel = stringResource(Res.string.tray_open)
        val trayMinimizeLabel = stringResource(Res.string.tray_minimize_to_tray)
        val trayExitLabel = stringResource(Res.string.tray_exit)
        val trayLockdownActiveLabel = stringResource(Res.string.lockdown_active)

        Tray(
            iconContent = {
                val isDark = isSystemInDarkMode()
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(Res.drawable.titleicon),
                        contentDescription = appName,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(if (isDark) Color.White else Color.Black),
                    )

                    trayBadgeState?.let {
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = it.description,
                            modifier = Modifier.size(40.dp).align(Alignment.TopEnd),
                            tint = it.iconColor,
                        )
                    }
                }
            },
            tooltip = appName,
            primaryAction = { handleWindowIntent(WindowIntent.SHOW) },
        ) {
            Item(label = trayOpenLabel) { handleWindowIntent(WindowIntent.SHOW) }
            Item(label = trayMinimizeLabel) { handleWindowIntent(WindowIntent.HIDE) }
            Item(label = trayExitLabel) { exitApplication() }
        }

        WGTunnelTheme(theme, useSystemColors, customSeedColor, accentStyle) {
            MaterialDecoratedWindow(
                visible = isMainWindowVisible,
                onCloseRequest = { handleWindowIntent(WindowIntent.HIDE) },
                title = appName + AppVariant.current.displaySuffix,
                resizable = true,
                icon = appIcon,
                state = windowState,
                minimumSize = DpSize(640.dp, 480.dp),
                nativePopupLayers = false,
                nativeContextMenu = true,
            ) {
                DisposableEffect(nucleusWindow) {
                    nucleusWindowRef = nucleusWindow
                    onDispose { nucleusWindowRef = null }
                }

                val density = LocalDensity.current
                LaunchedEffect(density.density) {
                    Logger.i {
                        "Tao window density=${density.density} fontScale=${density.fontScale}"
                    }
                }

                val isWindowFocused by nucleusWindow.focusFlow.collectAsState()

                LaunchedEffect(
                    windowState.isMinimized,
                    isMainWindowVisible,
                    isWindowFocused,
                ) {
                    when {
                        !isMainWindowVisible || windowState.isMinimized -> {
                            EnergyManager.disableLightEfficiencyMode()
                            EnergyManager.enableEfficiencyMode()
                        }

                        !isWindowFocused -> {
                            EnergyManager.disableEfficiencyMode()
                            EnergyManager.enableLightEfficiencyMode()
                        }

                        else -> {
                            EnergyManager.disableEfficiencyMode()
                            EnergyManager.disableLightEfficiencyMode()
                        }
                    }
                }

                KoinApplication(
                    configuration =
                        koinConfiguration(
                            declaration = {
                                val appVersionLabel =
                                    "${BuildConfig.APP_VERSION} (${AppVariant.current.id})"
                                modules(
                                    databaseModule,
                                    serviceModule(appVersionLabel),
                                    viewModelModule,
                                )
                            }
                        )
                ) {
                    val toaster = rememberToasterState()
                    val viewModel: AppViewModel = koinViewModel()
                    val uiState by viewModel.collectAsState()
                    val autoLaunchFailedLabel = stringResource(Res.string.autolaunch_update_failed)

                    LaunchedEffect(
                        uiState.theme,
                        uiState.useSystemColors,
                        uiState.customSeedColor,
                        uiState.accentStyle,
                    ) {
                        theme = uiState.theme
                        useSystemColors = uiState.useSystemColors
                        customSeedColor = uiState.customSeedColor?.let { Color(it) }
                        accentStyle = uiState.accentStyle
                    }

                    // Keeps the OS registration in sync with the saved preference - not just at
                    // startup, since toggling the setting should take effect immediately.
                    LaunchedEffect(uiState.isLoaded, uiState.launchAtLogin) {
                        if (!uiState.isLoaded) return@LaunchedEffect
                        val result =
                            withContext(Dispatchers.IO) {
                                if (uiState.launchAtLogin) AutoLaunch.enable()
                                else AutoLaunch.disable()
                            }
                        if (result != AutoLaunchResult.OK && result != AutoLaunchResult.UNCHANGED) {
                            Logger.w { "AutoLaunch sync returned $result" }
                            toaster.show(Toast(autoLaunchFailedLabel, type = ToastType.Warning))
                        }
                    }

                    // Restore features should only trigger if launch was actually triggered by
                    // the login mechanism, not a normal manual reopen
                    if (wasStartedAtLogin) {
                        val clientCacheRepository = koinInject<ClientCacheRepository>()
                        val tunnelRepository = koinInject<TunnelRepository>()
                        val tunnelCoordinator = koinInject<TunnelCoordinator>()
                        val autoTunnelRepository = koinInject<AutoTunnelSettingsRepository>()
                        var restoreAttempted by remember { mutableStateOf(false) }

                        LaunchedEffect(uiState.isLoaded) {
                            if (restoreAttempted || !uiState.isLoaded) return@LaunchedEffect
                            restoreAttempted = true

                            val autoTunnelSettings = autoTunnelRepository.get()
                            if (autoTunnelSettings.startOnBoot) {
                                if (!autoTunnelSettings.isAutoTunnelEnabled) {
                                    Logger.i { "Enabling auto-tunnel at login (startOnBoot)" }
                                    autoTunnelRepository.updateAutoTunnelEnabled(true)
                                }
                                return@LaunchedEffect
                            }

                            if (!uiState.restoreTunnelOnBoot) return@LaunchedEffect
                            val id = clientCacheRepository.getLastStartedTunnelId()
                            val config = id?.let { tunnelRepository.getById(it) }
                            if (config == null) {
                                Logger.i { "Nothing to restore at login (no cached tunnel)" }
                                return@LaunchedEffect
                            }
                            Logger.i { "Restoring tunnel ${config.name} at login" }
                            tunnelCoordinator.startTunnel(config).onFailure {
                                Logger.e(it) { "Failed to restore tunnel at login" }
                            }
                        }
                    }

                    val currentTunnelStatus by remember {
                        derivedStateOf {
                            uiState.tunnelStatuses.firstOrNull {
                                it.state == TunnelState.HANDSHAKE_FAILURE
                            }
                                ?: uiState.tunnelStatuses.firstOrNull {
                                    it.state == TunnelState.RESOLVING_DNS ||
                                        it.state == TunnelState.STOPPING ||
                                        it.state == TunnelState.STARTING
                                }
                                ?: uiState.tunnelStatuses.firstOrNull {
                                    it.state == TunnelState.HEALTHY
                                }
                        }
                    }

                    MaterialTitleBar(modifier = Modifier.newFullscreenControls()) { _ ->
                        Row(
                            modifier = Modifier.align(Alignment.Start).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.titleicon),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            )
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    val currentTunnelTooltipMessage = currentTunnelStatus?.state?.asTooltipMessage()

                    LaunchedEffect(
                        currentTunnelStatus,
                        currentTunnelTooltipMessage,
                        uiState.lockdownActive,
                    ) {
                        val status = currentTunnelStatus
                        trayBadgeState =
                            when {
                                uiState.lockdownActive && status == null ->
                                    TrayBadgeState(ErrorRed, trayLockdownActiveLabel)
                                status == null -> null
                                else ->
                                    TrayBadgeState(
                                        status.state.asColor(),
                                        currentTunnelTooltipMessage.orEmpty(),
                                    )
                            }
                    }

                    Surface(
                        modifier =
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column {
                            App(uiState, viewModel, toaster)
                            Toaster(
                                state = toaster,
                                richColors = true,
                                elevation = 1.dp,
                                shadowAmbientColor =
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shadowSpotColor =
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                border = { BorderStroke(0.dp, Color.Transparent) },
                                background = {
                                    SolidColor(
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    )
                                },
                                iconSlot = { toast ->
                                    val (icon, color) =
                                        when (toast.type) {
                                            ToastType.Success ->
                                                Icons.Outlined.CheckCircleOutline to HealthyGreen
                                            ToastType.Error ->
                                                Icons.Outlined.ErrorOutline to ErrorRed
                                            ToastType.Warning ->
                                                Icons.Outlined.WarningAmber to WarningAmber
                                            ToastType.Info,
                                            ToastType.Normal ->
                                                Icons.Outlined.Info to
                                                    MaterialTheme.colorScheme.onSurface
                                        }
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                },
                                messageSlot = { toast ->
                                    when (val message = toast.message) {
                                        is CommandToastMessage ->
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    message.description,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    message.command,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    modifier =
                                                        Modifier.background(
                                                                MaterialTheme.colorScheme
                                                                    .surfaceColorAtElevation(6.dp),
                                                                RoundedCornerShape(6.dp),
                                                            )
                                                            .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp,
                                                            ),
                                                )
                                            }
                                        else -> ToasterDefaults.messageSlot(toast)
                                    }
                                },
                                actionSlot = { toast ->
                                    // The library's own absolute-positioned closeButton slot
                                    // overlaps this row on desktop and never receives clicks, so
                                    // the dismiss X lives here instead, as a normal Row sibling.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        when (val action = toast.action) {
                                            is CopyCommandAction ->
                                                IconButton(
                                                    onClick = {
                                                        action.onClick(toast)
                                                        toaster.dismiss(toast.id)
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.ContentCopy,
                                                        contentDescription =
                                                            action.contentDescription,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            is NavigateAction ->
                                                IconButton(
                                                    onClick = {
                                                        action.onClick()
                                                        toaster.dismiss(toast.id)
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.InstallDesktop,
                                                        contentDescription =
                                                            action.contentDescription,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            else -> ToasterDefaults.actionSlot(toast)
                                        }
                                        IconButton(onClick = { toaster.dismiss(toast.id) }) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription =
                                                    stringResource(Res.string.dismiss),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                },
                                contentColor = { MaterialTheme.colorScheme.onSurface },
                                shape = { RoundedCornerShape(16.dp) },
                                containerPadding = PaddingValues(48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

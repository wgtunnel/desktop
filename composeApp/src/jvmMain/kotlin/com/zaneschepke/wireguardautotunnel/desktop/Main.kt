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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.wgtunnel.backend.BackendLog
import com.wgtunnel.backend.LogLevel
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.di.databaseModule
import com.zaneschepke.wireguardautotunnel.client.di.serviceModule
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.appicon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.titleicon
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.desktop.di.viewModelModule
import com.zaneschepke.wireguardautotunnel.desktop.ui.WindowIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asColor
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asTooltipMessage
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TrayBadgeState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WGTunnelTheme
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.NucleusWindow
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.composenativetray.tray.api.Tray
import dev.nucleusframework.core.runtime.SingleInstanceManager
import dev.nucleusframework.energymanager.EnergyManager
import dev.nucleusframework.window.material.MaterialDecoratedWindow
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.nucleusframework.window.newFullscreenControls
import java.nio.file.Paths
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
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

    nucleusApplication(args = args, backend = NucleusBackend.Tao) {
        var nucleusWindowRef: NucleusWindow? by remember { mutableStateOf(null) }
        var isMainWindowVisible by remember { mutableStateOf(true) }

        var theme by remember { mutableStateOf(Theme.DARK) }
        var useSystemColors by remember { mutableStateOf(false) }

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

        LaunchedEffect(windowState.isMinimized) {
            if (windowState.isMinimized) {
                handleWindowIntent(WindowIntent.HIDE)
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

        Tray(
            iconContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = appIcon,
                        contentDescription = appName,
                        modifier = Modifier.fillMaxSize(),
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
            Item(label = "Open WG Tunnel") { handleWindowIntent(WindowIntent.SHOW) }
            Item(label = "Minimize to Tray") { handleWindowIntent(WindowIntent.HIDE) }
            Item(label = "Exit") { exitApplication() }
        }

        WGTunnelTheme(theme, useSystemColors) {
            MaterialDecoratedWindow(
                visible = isMainWindowVisible,
                onCloseRequest = ::exitApplication,
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
                                modules(databaseModule, serviceModule, viewModelModule)
                            }
                        )
                ) {
                    val toaster = rememberToasterState()
                    val viewModel: AppViewModel = koinViewModel()
                    val uiState by viewModel.collectAsState()

                    LaunchedEffect(uiState.theme, uiState.useSystemColors) {
                        theme = uiState.theme
                        useSystemColors = uiState.useSystemColors
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

                    LaunchedEffect(uiState.tunnelStatuses, uiState.lockdownActive) {
                        val state =
                            uiState.tunnelStatuses
                                .firstOrNull { it.state == TunnelState.HANDSHAKE_FAILURE }
                                ?.state
                                ?: uiState.tunnelStatuses
                                    .firstOrNull {
                                        it.state == TunnelState.RESOLVING_DNS ||
                                            it.state == TunnelState.STOPPING ||
                                            it.state == TunnelState.STARTING
                                    }
                                    ?.state
                                ?: uiState.tunnelStatuses
                                    .firstOrNull { it.state == TunnelState.HEALTHY }
                                    ?.state
                                ?: uiState.tunnelStatuses
                                    .firstOrNull { it.state == TunnelState.DOWN }
                                    ?.state

                        trayBadgeState =
                            when {
                                uiState.lockdownActive &&
                                    (state == null || state == TunnelState.DOWN) ->
                                    TrayBadgeState(ErrorRed, "Lockdown active")
                                state == null || state == TunnelState.DOWN -> null
                                else -> TrayBadgeState(state.asColor(), state.asTooltipMessage())
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
                                elevation = 0.dp,
                                border = { BorderStroke(0.dp, Color.Transparent) },
                                background = {
                                    SolidColor(MaterialTheme.colorScheme.inverseOnSurface)
                                },
                                iconSlot = {
                                    Icon(
                                        when (it.type) {
                                            ToastType.Normal,
                                            ToastType.Info -> Icons.Default.Info

                                            ToastType.Success -> Icons.Default.Check
                                            ToastType.Warning -> Icons.Default.Warning
                                            ToastType.Error -> Icons.Default.Error
                                        },
                                        null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.inverseSurface,
                                    )
                                },
                                messageSlot = {
                                    val message = it.message as? String ?: return@Toaster
                                    Text(
                                        message,
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                },
                                contentColor = { MaterialTheme.colorScheme.inverseSurface },
                                shape = { RoundedCornerShape(8.dp) },
                                containerPadding = PaddingValues(48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

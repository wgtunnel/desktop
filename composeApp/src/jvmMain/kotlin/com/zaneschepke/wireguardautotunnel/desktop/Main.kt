package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.di.databaseModule
import com.zaneschepke.wireguardautotunnel.client.di.serviceModule
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.appicon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.titleicon
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.desktop.di.viewModelModule
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asColor
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asTooltipMessage
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TrayBadgeState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WGTunnelTheme
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import io.github.kdroidfilter.nucleus.hidpi.getLinuxNativeScaleFactor
import io.github.kdroidfilter.nucleus.window.material.MaterialDecoratedWindow
import io.github.kdroidfilter.nucleus.window.material.MaterialTitleBar
import io.github.kdroidfilter.nucleus.window.newFullscreenControls
import java.nio.file.Paths
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalTrayAppApi::class)
fun main() {
    System.setProperty("skiko.renderApi", "SOFTWARE_FASTEST")

    // HiDPI detection for Linux
    if (System.getProperty("sun.java2d.uiScale") == null) {
        val scale = getLinuxNativeScaleFactor()
        if (scale > 0.0) {
            System.setProperty("sun.java2d.uiScale", scale.toString())
        }
    }

    application {
        var isWindowVisible by remember { mutableStateOf(false) }
        var theme by remember { mutableStateOf(Theme.DARK) }
        var useSystemColors by remember { mutableStateOf(false) }

        SingleInstanceManager.configuration =
            SingleInstanceManager.Configuration(
                lockFilesDir = Paths.get(FilePathsHelper.getDatabaseDir().path),
                lockIdentifier = "wg_tunnel",
            )
        val isSingleInstance =
            SingleInstanceManager.isSingleInstance(onRestoreRequest = { isWindowVisible = true })

        if (!isSingleInstance) {
            exitApplication()
            return@application
        }

        var trayBadgeState: TrayBadgeState? by remember { mutableStateOf(null) }

        KoinApplication(
            configuration =
                koinConfiguration(
                    declaration = { modules(databaseModule, serviceModule, viewModelModule) }
                )
        ) {
            val appIcon = painterResource(Res.drawable.appicon)
            val appName = stringResource(Res.string.app_name)

            val windowState = rememberWindowState(size = DpSize(1000.dp, 700.dp))
            val toaster = rememberToasterState()

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
                primaryAction = { isWindowVisible = true },
            ) {
                if (!isWindowVisible) {
                    Item(label = "Open") {
                        Logger.i { "Open menu item clicked" }
                        isWindowVisible = true
                    }
                }
                Item(label = "Exit") { exitApplication() }
            }

            WGTunnelTheme(theme, useSystemColors) {
                MaterialDecoratedWindow(
                    visible = isWindowVisible,
                    onCloseRequest = {
                        isWindowVisible = false
                        Logger.i { "OS close requested - hiding to tray" }
                    },
                    title = appName,
                    resizable = false,
                    icon = appIcon,
                    state = windowState,
                ) {
                    //                    window.minimumSize = java.awt.Dimension(950, 650)
                    val viewModel: AppViewModel = koinViewModel()
                    val uiState by viewModel.collectAsState()

                    LaunchedEffect(uiState.theme, uiState.useSystemColors) {
                        theme = uiState.theme
                        useSystemColors = uiState.useSystemColors
                        if (!isWindowVisible) isWindowVisible = true
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
                        if (uiState.tunnelStatuses.isEmpty() && !uiState.lockdownActive) {
                            trayBadgeState = null
                            return@LaunchedEffect
                        }

                        val state: TunnelState? =
                            (uiState.tunnelStatuses.firstOrNull {
                                    it.state == TunnelState.HANDSHAKE_FAILURE
                                }
                                    ?: uiState.tunnelStatuses.firstOrNull {
                                        it.state == TunnelState.RESOLVING_DNS ||
                                            it.state == TunnelState.STOPPING ||
                                            it.state == TunnelState.STARTING
                                    }
                                    ?: uiState.tunnelStatuses.firstOrNull {
                                        it.state == TunnelState.HEALTHY
                                    })
                                ?.state

                        val (color, description) =
                            when {
                                uiState.lockdownActive && state == null ->
                                    ErrorRed to "Lockdown active"
                                else -> state!!.asColor() to state.asTooltipMessage()
                            }
                        trayBadgeState = TrayBadgeState(color, description)
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

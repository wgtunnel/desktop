package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.kdroid.composetray.utils.isMenuBarInDarkMode
import com.zaneschepke.wireguardautotunnel.client.di.databaseModule
import com.zaneschepke.wireguardautotunnel.client.di.serviceModule
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wgtunnel
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.desktop.di.viewModelModule
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.bar.TitleBar
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WGTunnelTheme
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import java.nio.file.Paths
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalTrayAppApi::class)
fun main() = application {
    var isWindowVisible by remember { mutableStateOf(true) }
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

    KoinApplication(
        configuration =
            koinConfiguration(
                declaration = { modules(databaseModule, serviceModule, viewModelModule) }
            )
    ) {
        val appIcon = painterResource(Res.drawable.wgtunnel)
        val appName = stringResource(Res.string.app_name)

        val isMenuBarDark = isMenuBarInDarkMode()
        val windowState = rememberWindowState(size = DpSize(800.dp, 650.dp))
        val toaster = rememberToasterState()

        Tray(
            iconContent = {
                Icon(
                    imageVector = vectorResource(Res.drawable.wgtunnel),
                    contentDescription = appName,
                    tint = if (isMenuBarDark) Color.White else Color.Black,
                )
            },
            tooltip = appName,
        ) {
            if (!isWindowVisible) {
                Item(label = "Open") {
                    Logger.i { "Open menu item clicked" }
                    isWindowVisible = true
                }
            }
            Item(label = "Exit") { exitApplication() }
        }

        Window(
            visible = isWindowVisible,
            onCloseRequest = {
                isWindowVisible = false
                Logger.i { "OS close requested - hiding to tray" }
            },
            title = stringResource(Res.string.app_name),
            icon = appIcon,
            state = windowState,
            undecorated = true,
            transparent = true,
        ) {
            window.minimumSize = java.awt.Dimension(800, 650)

            val viewModel: AppViewModel = koinViewModel()
            val uiState by viewModel.collectAsState()

            WGTunnelTheme(uiState.theme) {
                Surface(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column {
                        TitleBar(onClose = { isWindowVisible = false })
                        App(uiState, viewModel, toaster)
                        Toaster(
                            state = toaster,
                            elevation = 0.dp,
                            border = { BorderStroke(0.dp, Color.Transparent) },
                            background = { SolidColor(MaterialTheme.colorScheme.inverseOnSurface) },
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

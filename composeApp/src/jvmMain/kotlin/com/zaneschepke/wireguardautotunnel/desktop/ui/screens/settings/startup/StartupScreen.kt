package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.launch_at_login
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.launch_at_login_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.launch_at_login_locked_reason
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_auto_tunnel_at_login
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_auto_tunnel_at_login_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_auto_tunnel_at_login_locked_reason
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_tunnel_at_login
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_tunnel_at_login_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.startup
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.DisabledReasonTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun StartupScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(title = stringResource(Res.string.startup)) { padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val launchAtLoginLocked =
                uiState.settings.restoreTunnelOnBoot || uiState.autoTunnel.startOnBoot
            val autoTunnelStartOnBootLocked = uiState.settings.restoreTunnelOnBoot
            Column {
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null) },
                    title = stringResource(Res.string.launch_at_login),
                    trailing = {
                        DisabledReasonTooltip(
                            enabled = !launchAtLoginLocked,
                            reason = stringResource(Res.string.launch_at_login_locked_reason),
                        ) {
                            ThemedSwitch(
                                checked = uiState.settings.launchAtLogin,
                                enabled = !launchAtLoginLocked,
                                onClick = { viewModel.onLaunchAtLogin(it) },
                            )
                        }
                    },
                    description = {
                        DescriptionText(stringResource(Res.string.launch_at_login_desc))
                    },
                    enabled = !launchAtLoginLocked,
                    onClick = { viewModel.onLaunchAtLogin(!uiState.settings.launchAtLogin) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                    title = stringResource(Res.string.restore_tunnel_at_login),
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.settings.restoreTunnelOnBoot,
                            onClick = { viewModel.onRestoreTunnelOnBoot(it) },
                        )
                    },
                    description = {
                        DescriptionText(stringResource(Res.string.restore_tunnel_at_login_desc))
                    },
                    onClick = {
                        viewModel.onRestoreTunnelOnBoot(!uiState.settings.restoreTunnelOnBoot)
                    },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                    title = stringResource(Res.string.restore_auto_tunnel_at_login),
                    trailing = {
                        DisabledReasonTooltip(
                            enabled = !autoTunnelStartOnBootLocked,
                            reason =
                                stringResource(
                                    Res.string.restore_auto_tunnel_at_login_locked_reason
                                ),
                        ) {
                            ThemedSwitch(
                                checked = uiState.autoTunnel.startOnBoot,
                                enabled = !autoTunnelStartOnBootLocked,
                                onClick = { viewModel.onAutoTunnelStartOnBoot(it) },
                            )
                        }
                    },
                    description = {
                        DescriptionText(
                            stringResource(Res.string.restore_auto_tunnel_at_login_desc)
                        )
                    },
                    enabled = !autoTunnelStartOnBootLocked,
                    onClick = {
                        viewModel.onAutoTunnelStartOnBoot(!uiState.autoTunnel.startOnBoot)
                    },
                )
            }
        }
    }
}

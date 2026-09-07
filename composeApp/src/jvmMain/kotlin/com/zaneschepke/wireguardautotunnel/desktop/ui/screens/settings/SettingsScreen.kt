package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.appearance
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.backend_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.current_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.general
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kill_switch_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kill_switch_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.local_logging
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.local_logging_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.requires_daemon_running
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_tunnel_on_boot
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.sdk
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_globals
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_globals_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_monitoring
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_recovery
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.PreferenceTrailing
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.OptionPickerMenu
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.PickerOption
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.DisabledReasonTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.util.asDescription
import com.zaneschepke.wireguardautotunnel.desktop.util.asIcon
import com.zaneschepke.wireguardautotunnel.desktop.util.asTitleString
import com.zaneschepke.wireguardautotunnel.desktop.util.desktopTunnelModes
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()
    var showModeMenu by rememberSaveable { mutableStateOf(false) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    val appMode = uiState.tunnelMode

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings)) }) }) {
        padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier =
                Modifier.verticalScroll(rememberScrollState()).fillMaxSize().padding(padding),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.tunnel),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Box {
                    SurfaceRow(
                        leading = {
                            Icon(
                                vectorResource(Res.drawable.sdk),
                                contentDescription = null,
                            )
                        },
                        trailing = { modifier ->
                            val expand =
                                @Composable {
                                    IconButton(
                                        onClick = { showModeMenu = true },
                                        modifier = modifier,
                                    ) {
                                        Icon(
                                            Icons.Outlined.ExpandMore,
                                            contentDescription = stringResource(Res.string.select),
                                        )
                                    }
                                }
                            when (appMode) {
                                TunnelMode.VPN -> expand()
                                TunnelMode.PROXY -> PreferenceTrailing { expand() }
                            }
                        },
                        title = stringResource(Res.string.backend_mode),
                        description = {
                            DescriptionText(
                                stringResource(Res.string.current_template, appMode.asTitleString())
                            )
                        },
                        onClick = {
                            when (appMode) {
                                TunnelMode.PROXY -> navController.push(Route.ProxySettings)
                                else -> showModeMenu = true
                            }
                        },
                    )
                    OptionPickerMenu(
                        expanded = showModeMenu,
                        onDismiss = { showModeMenu = false },
                        options =
                            desktopTunnelModes().map { mode ->
                                PickerOption(
                                    leadingIcon = mode.asIcon(),
                                    label = mode.asTitleString(),
                                    description = mode.asDescription(),
                                    selected = appMode == mode,
                                    onClick = {
                                        showModeMenu = false
                                        viewModel.onTunnelMode(mode)
                                    },
                                )
                            },
                    )
                }
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                    title = stringResource(Res.string.dns_settings),
                    onClick = { navController.push(Route.Dns) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Public, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_globals),
                    description = {
                        DescriptionText(stringResource(Res.string.tunnel_globals_desc))
                    },
                    onClick = { navController.push(Route.TunnelGlobals) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    title = stringResource(Res.string.kill_switch_label),
                    description = { DescriptionText(stringResource(Res.string.kill_switch_desc)) },
                    trailing = { modifier ->
                        DisabledReasonTooltip(
                            enabled = uiState.daemonConnected,
                            reason = stringResource(Res.string.requires_daemon_running),
                            modifier = modifier,
                        ) {
                            SwitchWithDivider(
                                checked = uiState.lockdownEnabled,
                                enabled = uiState.daemonConnected,
                                onClick = { viewModel.onKillSwitchEnabled(it) },
                            )
                        }
                    },
                    onClick = { navController.push(Route.LockdownSettings) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_recovery),
                    onClick = { navController.push(Route.TunnelRecovery) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.MonitorHeart, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_monitoring),
                    onClick = { navController.push(Route.TunnelMonitoring) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                    title = stringResource(Res.string.restore_tunnel_on_boot),
                    trailing = {
                        DisabledReasonTooltip(
                            enabled = uiState.daemonConnected,
                            reason = stringResource(Res.string.requires_daemon_running),
                        ) {
                            ThemedSwitch(
                                checked = uiState.settings.restoreTunnelOnBoot,
                                enabled = uiState.daemonConnected,
                                onClick = { viewModel.onRestoreTunnelOnBoot(it) },
                            )
                        }
                    },
                    enabled = uiState.daemonConnected,
                    onClick = {
                        viewModel.onRestoreTunnelOnBoot(!uiState.settings.restoreTunnelOnBoot)
                    },
                )
            }
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                GroupLabel(
                    stringResource(Res.string.general),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = {
                        Icon(Icons.AutoMirrored.Outlined.ViewQuilt, contentDescription = null)
                    },
                    title = stringResource(Res.string.appearance),
                    onClick = { navController.push(Route.Appearance) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.ViewHeadline, contentDescription = null) },
                    title = stringResource(Res.string.local_logging),
                    trailing = { modifier ->
                        SwitchWithDivider(
                            checked = uiState.monitoring.isLocalLogsEnabled,
                            onClick = { viewModel.setLocalLogging(it) },
                            modifier = modifier,
                        )
                    },
                    description = {
                        DescriptionText(stringResource(Res.string.local_logging_desc))
                    },
                    onClick = { navController.push(Route.Logs) },
                )
            }
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources._default
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.active_network
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.auto_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.auto_tunnel_not_running
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.auto_tunnel_running
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.automation
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bssid
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ethernet
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.globe
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.mapped
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.network_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.networks
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_network
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.preferred_tunnel_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restart_at_boot
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.start
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.start_on_boot_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stop
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stop_on_no_internet
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stop_on_no_internet_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_on_ethernet
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_on_wifi
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wifi
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.RequiresDaemonTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AutoTunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTunnelScreen(viewModel: AutoTunnelViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    val ethernetTunnel =
        remember(uiState.tunnels) { uiState.tunnels.firstOrNull { it.isEthernetTunnel } }
    val mappedTunnels =
        remember(uiState.tunnels) { uiState.tunnels.any { it.tunnelNetworks.isNotEmpty() } }

    val networkType =
        if (!uiState.daemonConnected) {
            stringResource(Res.string.unknown).replaceFirstChar { it.titlecase() }
        } else {
            when (uiState.network.type.lowercase()) {
                "wifi" -> stringResource(Res.string.wifi)
                "ethernet" -> stringResource(Res.string.ethernet)
                "disconnected",
                "" -> stringResource(Res.string.no_network)
                else -> uiState.network.type
            }
        }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.auto_tunnel)) }) }) {
        padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val (title, buttonText, icon) =
                if (uiState.autoTunnelActive) {
                    Triple(
                        stringResource(Res.string.auto_tunnel_running),
                        stringResource(Res.string.stop),
                        Icons.Outlined.CheckCircle,
                    )
                } else {
                    Triple(
                        stringResource(Res.string.auto_tunnel_not_running),
                        stringResource(Res.string.start),
                        Icons.Outlined.Info,
                    )
                }
            SurfaceRow(
                leading = { Icon(icon, null) },
                title = title,
                trailing = {
                    RequiresDaemonTooltip(daemonConnected = uiState.daemonConnected) {
                        Button(
                            onClick = { viewModel.toggleAutoTunnel() },
                            enabled = uiState.daemonConnected,
                        ) {
                            Text(
                                buttonText,
                                fontWeight = FontWeight.Bold,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.surface
                                    ),
                            )
                        }
                    }
                },
                enabled = uiState.daemonConnected,
                onClick = { viewModel.toggleAutoTunnel() },
            )
            Column {
                GroupLabel(
                    stringResource(Res.string.networks),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = {
                        RequiresDaemonTooltip(daemonConnected = uiState.daemonConnected) {
                            Icon(vectorResource(Res.drawable.globe), contentDescription = null)
                        }
                    },
                    title =
                        buildAnnotatedString {
                            append(stringResource(Res.string.active_network))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(networkType)
                            }
                        },
                    enabled = uiState.daemonConnected,
                    description =
                        if (
                            uiState.daemonConnected &&
                                uiState.network.type.equals("wifi", ignoreCase = true)
                        ) {
                            {
                                SelectionContainer {
                                    Column {
                                        if (uiState.network.ssid.isNotBlank()) {
                                            DescriptionText(
                                                buildAnnotatedString {
                                                    append(stringResource(Res.string.network_name))
                                                    withStyle(
                                                        style =
                                                            SpanStyle(fontWeight = FontWeight.Bold)
                                                    ) {
                                                        append(uiState.network.ssid)
                                                    }
                                                }
                                            )
                                        }
                                        if (uiState.network.bssid.isNotBlank()) {
                                            DescriptionText(
                                                buildAnnotatedString {
                                                    append(stringResource(Res.string.bssid))
                                                    append(": ")
                                                    withStyle(
                                                        style =
                                                            SpanStyle(fontWeight = FontWeight.Bold)
                                                    ) {
                                                        append(uiState.network.bssid)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else null,
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_on_wifi),
                    trailing = { modifier ->
                        SwitchWithDivider(
                            checked = uiState.autoTunnelSettings.isTunnelOnWifiEnabled,
                            onClick = { viewModel.setTunnelOnWifi(it) },
                            modifier = modifier,
                        )
                    },
                    description = {
                        DescriptionText(
                            buildAnnotatedString {
                                append(stringResource(Res.string.preferred_tunnel_label))
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(
                                        if (mappedTunnels) stringResource(Res.string.mapped)
                                        else stringResource(Res.string._default)
                                    )
                                }
                            }
                        )
                    },
                    onClick = { navController.push(Route.WifiPreferences) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_on_ethernet),
                    trailing = { modifier ->
                        SwitchWithDivider(
                            checked = uiState.autoTunnelSettings.isTunnelOnEthernetEnabled,
                            onClick = { viewModel.setTunnelOnEthernet(it) },
                            modifier = modifier,
                        )
                    },
                    description = {
                        DescriptionText(
                            buildAnnotatedString {
                                append(stringResource(Res.string.preferred_tunnel_label))
                                ethernetTunnel?.name?.let { append(it) }
                                    ?: withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(Res.string._default))
                                    }
                            }
                        )
                    },
                    onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.ETHERNET)) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.PublicOff, contentDescription = null) },
                    title = stringResource(Res.string.stop_on_no_internet),
                    description = {
                        DescriptionText(stringResource(Res.string.stop_on_no_internet_desc))
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.autoTunnelSettings.isStopOnNoInternetEnabled,
                            onClick = { viewModel.setStopOnNoInternet(it) },
                        )
                    },
                    onClick = {
                        viewModel.setStopOnNoInternet(
                            !uiState.autoTunnelSettings.isStopOnNoInternetEnabled
                        )
                    },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.automation),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                    title = stringResource(Res.string.restart_at_boot),
                    trailing = {
                        RequiresDaemonTooltip(daemonConnected = uiState.daemonConnected) {
                            ThemedSwitch(
                                checked = uiState.autoTunnelSettings.startOnBoot,
                                enabled = uiState.daemonConnected,
                                onClick = { viewModel.setStartOnBoot(it) },
                            )
                        }
                    },
                    description = {
                        DescriptionText(stringResource(Res.string.start_on_boot_desc))
                    },
                    enabled = uiState.daemonConnected,
                    onClick = { viewModel.setStartOnBoot(!uiState.autoTunnelSettings.startOnBoot) },
                )
            }
        }
    }
}

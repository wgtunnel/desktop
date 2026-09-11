package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.preferred

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.preferred_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.trusted_bssid
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.trusted_wifi_names
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.components.NetworkRuleInput
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AutoTunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun PreferredTunnelScreen(
    tunnelNetwork: TunnelNetwork,
    viewModel: AutoTunnelViewModel = koinViewModel(),
) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()
    var currentSsidText by rememberSaveable { mutableStateOf("") }
    var currentBssidText by rememberSaveable { mutableStateOf("") }
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    if (!uiState.isLoaded) return

    val currentSelection =
        remember(uiState.tunnels, tunnelNetwork) {
            when (tunnelNetwork) {
                TunnelNetwork.ETHERNET -> uiState.tunnels.firstOrNull { it.isEthernetTunnel }
                TunnelNetwork.WIFI -> selectedTunnel
            }
        }

    NestedSettingsScaffold(title = stringResource(Res.string.preferred_tunnel)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (tunnelNetwork == TunnelNetwork.ETHERNET) {
                items(uiState.tunnels, key = { it.id }) { tunnel ->
                    val selected = currentSelection?.id == tunnel.id
                    SurfaceRow(
                        title = tunnel.name,
                        selected = selected,
                        trailing = {
                            if (selected) Icon(Icons.Outlined.Check, contentDescription = null)
                        },
                        onClick = { viewModel.setEthernetTunnel(if (selected) null else tunnel) },
                    )
                }
            } else {
                items(uiState.tunnels, key = { it.id }) { tunnel ->
                    val expanded = selectedTunnel?.id == tunnel.id
                    Column {
                        SurfaceRow(
                            title = tunnel.name,
                            selected = expanded || tunnel.tunnelNetworks.isNotEmpty(),
                            onClick = { selectedTunnel = if (expanded) null else tunnel },
                        )
                        if (expanded) {
                            NetworkRuleInput(
                                inputTitle = stringResource(Res.string.trusted_wifi_names),
                                placeholder = viewModel.ssidHints.firstOrNull().orEmpty(),
                                rules = tunnel.tunnelNetworks,
                                onDelete = { viewModel.removeWifiMapping(tunnel, it) },
                                currentText = currentSsidText,
                                onValueChange = { currentSsidText = it },
                                onSave = {
                                    viewModel.saveWifiMapping(tunnel, it)
                                    currentSsidText = ""
                                },
                            )
                            NetworkRuleInput(
                                inputTitle = stringResource(Res.string.trusted_bssid),
                                placeholder = viewModel.bssidHints.firstOrNull().orEmpty(),
                                rules = tunnel.tunnelBssids,
                                onDelete = { viewModel.removeBssidMapping(tunnel, it) },
                                currentText = currentBssidText,
                                onValueChange = { currentBssidText = it },
                                onSave = {
                                    viewModel.saveBssidMapping(tunnel, it)
                                    currentBssidText = ""
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

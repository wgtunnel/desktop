package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.entry_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.entry_tunnel_none
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.entry_tunnel_none_desc
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.TunnelPickerList
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun EntryTunnelScreen(viewModel: TunnelViewModel) {
    val toaster = LocalToaster.current
    val navController = LocalNavController.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    if (!uiState.isLoaded) return
    val tunnel = uiState.currentConfig

    val candidates =
        remember(uiState.userTunnels, tunnel.id) {
            uiState.userTunnels.filter { it.id != tunnel.id && it.entryTunnelId == null }
        }

    NestedSettingsScaffold(title = stringResource(Res.string.entry_tunnel)) { padding ->
        TunnelPickerList(
            modifier = Modifier.padding(padding),
            tunnels = candidates,
            isSelected = { it.id == tunnel.entryTunnelId },
            onSelect = {
                viewModel.onEntryTunnel(it.id)
                navController.pop()
            },
            leading = { Icon(Icons.AutoMirrored.Outlined.AltRoute, contentDescription = null) },
            leadingItem = {
                val noneSelected = tunnel.entryTunnelId == null
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.LinkOff, contentDescription = null) },
                    title = stringResource(Res.string.entry_tunnel_none),
                    description = {
                        DescriptionText(stringResource(Res.string.entry_tunnel_none_desc))
                    },
                    selected = noneSelected,
                    trailing = {
                        if (noneSelected) Icon(Icons.Outlined.Check, contentDescription = null)
                    },
                    onClick = {
                        viewModel.onEntryTunnel(null)
                        navController.pop()
                    },
                )
            },
        )
    }
}

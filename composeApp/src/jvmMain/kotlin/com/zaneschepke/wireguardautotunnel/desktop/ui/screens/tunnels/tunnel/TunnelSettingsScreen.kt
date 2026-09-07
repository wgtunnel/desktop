package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.automation
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ddns_auto_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ddns_auto_update_description
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.general
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ipv6_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.prefer_ipv6
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.prefer_ipv6_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.primary_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_ipv6
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restore_ipv6_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.set_primary_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.view_configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.view_live_tunnel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelSettingsScreen(viewModel: TunnelViewModel) {
    val toaster = LocalToaster.current
    val navController = LocalNavController.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return
    val tunnel = uiState.currentConfig

    NestedSettingsScaffold(
        showSave = uiState.isDirty,
        onSave = viewModel::saveChanges,
        titleContent = {
            BasicTextField(
                value = uiState.editedConfig.name,
                onValueChange = { viewModel.onNameUpdated(it) },
                textStyle =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.configuration),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    title = stringResource(Res.string.view_configuration),
                    onClick = { navController.push(Route.Config(tunnel.id)) },
                )
                if (uiState.activeConfig != null) {
                    SurfaceRow(
                        leading = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
                        title = stringResource(Res.string.view_live_tunnel),
                        onClick = { navController.push(Route.LiveConfig(tunnel.id)) },
                    )
                }
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.general),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Star, contentDescription = null) },
                    title = stringResource(Res.string.primary_tunnel),
                    description = {
                        DescriptionText(stringResource(Res.string.set_primary_tunnel))
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = tunnel.isPrimaryTunnel,
                            onClick = { viewModel.togglePrimaryTunnel() },
                        )
                    },
                    onClick = { viewModel.togglePrimaryTunnel() },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.ipv6_settings),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Public, contentDescription = null) },
                    title = stringResource(Res.string.prefer_ipv6),
                    trailing = {
                        ThemedSwitch(
                            checked = tunnel.preferIpv6,
                            onClick = { viewModel.onIpv6Preferred(it) },
                        )
                    },
                    description = { DescriptionText(stringResource(Res.string.prefer_ipv6_desc)) },
                    onClick = { viewModel.onIpv6Preferred(!tunnel.preferIpv6) },
                )
                val ipv6Enabled = tunnel.preferIpv6
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                    title = stringResource(Res.string.restore_ipv6),
                    enabled = ipv6Enabled,
                    description = { DescriptionText(stringResource(Res.string.restore_ipv6_desc)) },
                    trailing = {
                        ThemedSwitch(
                            checked = tunnel.ipv6RestoreEnabled,
                            onClick = { viewModel.onIpv6Restore(it) },
                            enabled = ipv6Enabled,
                        )
                    },
                    onClick = { viewModel.onIpv6Restore(!tunnel.ipv6RestoreEnabled) },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.automation),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                    title = stringResource(Res.string.ddns_auto_update),
                    description = {
                        DescriptionText(stringResource(Res.string.ddns_auto_update_description))
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = tunnel.isDdnsTunnel,
                            onClick = { viewModel.onDdnsTunnel(it) },
                        )
                    },
                    onClick = { viewModel.onDdnsTunnel(!tunnel.isDdnsTunnel) },
                )
            }
        }
    }
}

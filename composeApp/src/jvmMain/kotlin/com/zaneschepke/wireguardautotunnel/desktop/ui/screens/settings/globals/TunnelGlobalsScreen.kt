package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.globals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_globals
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState

@Composable
fun TunnelGlobalsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val uiState by viewModel.collectAsState()
    if (!uiState.isLoaded) return

    NestedSettingsScaffold(title = stringResource(Res.string.tunnel_globals)) { padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Description, contentDescription = null) },
                title = stringResource(Res.string.tunnel_configuration),
                onClick = {
                    uiState.globalTunnelConfig?.let {
                        navController.push(Route.ConfigGlobal(it.id))
                    }
                },
            )
        }
    }
}

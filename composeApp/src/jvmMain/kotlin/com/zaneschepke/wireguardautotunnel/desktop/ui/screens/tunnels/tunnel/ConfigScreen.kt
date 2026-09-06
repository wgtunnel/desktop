package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.live_configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_active_tunnel_data
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.view_configuration
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components.ConfigEditor
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ConfigScreen(viewModel: TunnelViewModel, live: Boolean) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(
        title =
            if (live) stringResource(Res.string.live_configuration)
            else stringResource(Res.string.view_configuration),
        showSave = !live && uiState.isDirty,
        onSave = viewModel::saveChanges,
    ) { padding ->
        val raw =
            if (live) {
                uiState.activeConfig?.asQuickString()
                    ?: stringResource(Res.string.no_active_tunnel_data)
            } else {
                uiState.editedConfig.quickConfig
            }
        ConfigEditor(
            rawConfig = raw,
            isEditable = !live,
            onConfigChange = { if (!live) viewModel.onConfigUpdate(it) },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.globals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HdrAuto
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.amnezia_configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_servers
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_configuration
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components.ConfigEditor
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.GlobalConfigViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun GlobalConfigScreen(viewModel: GlobalConfigViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(
        title = stringResource(Res.string.tunnel_configuration),
        showSave = uiState.isDirty,
        onSave = viewModel::saveChanges,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                title = stringResource(Res.string.dns_servers),
                trailing = { modifier ->
                    ThemedSwitch(
                        checked = uiState.dnsEnabled,
                        onClick = { viewModel.setDnsEnabled(it) },
                        modifier = modifier,
                    )
                },
                onClick = { viewModel.setDnsEnabled(!uiState.dnsEnabled) },
            )
            SurfaceRow(
                leading = { Icon(Icons.Outlined.HdrAuto, contentDescription = null) },
                title = stringResource(Res.string.amnezia_configuration),
                trailing = { modifier ->
                    ThemedSwitch(
                        checked = uiState.amneziaEnabled,
                        onClick = { viewModel.setAmneziaEnabled(it) },
                        modifier = modifier,
                    )
                },
                onClick = { viewModel.setAmneziaEnabled(!uiState.amneziaEnabled) },
            )
            if (uiState.showEditor) {
                ConfigEditor(
                    rawConfig = uiState.editorText,
                    isEditable = true,
                    onConfigChange = viewModel::onEditorChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

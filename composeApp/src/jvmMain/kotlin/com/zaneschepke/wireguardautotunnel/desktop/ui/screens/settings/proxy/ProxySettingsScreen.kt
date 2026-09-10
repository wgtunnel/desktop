package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.proxy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward5
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.credentials_encrypted_at_rest
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.hide_password
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.http_bind_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.http_proxy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.password
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.proxy_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.show_password
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.socks5_bind_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.socks5_proxy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.username
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.ProxyViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySettingsScreen(viewModel: ProxyViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(
        title = stringResource(Res.string.proxy_settings),
        showSave = uiState.isDirty,
        onSave = viewModel::save,
    ) { padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Forward5, contentDescription = null) },
                    title = stringResource(Res.string.socks5_proxy),
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.draft.socks5ProxyEnabled,
                            onClick = viewModel::onSocks5Enabled,
                        )
                    },
                    onClick = { viewModel.onSocks5Enabled(!uiState.draft.socks5ProxyEnabled) },
                )
                if (uiState.draft.socks5ProxyEnabled) {
                    ConfigurationTextBox(
                        value = uiState.socksBindAddress,
                        onValueChange = viewModel::onSocksBindChanged,
                        label = stringResource(Res.string.socks5_bind_address),
                        hint = ProxySettings.DEFAULT_SOCKS_BIND_ADDRESS,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Http, contentDescription = null) },
                    title = stringResource(Res.string.http_proxy),
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.draft.httpProxyEnabled,
                            onClick = viewModel::onHttpEnabled,
                        )
                    },
                    onClick = { viewModel.onHttpEnabled(!uiState.draft.httpProxyEnabled) },
                )
                if (uiState.draft.httpProxyEnabled) {
                    ConfigurationTextBox(
                        value = uiState.httpBindAddress,
                        onValueChange = viewModel::onHttpBindChanged,
                        label = stringResource(Res.string.http_bind_address),
                        hint = ProxySettings.DEFAULT_HTTP_BIND_ADDRESS,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (uiState.draft.socks5ProxyEnabled || uiState.draft.httpProxyEnabled) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    GroupLabel(stringResource(Res.string.credentials_encrypted_at_rest))
                    ConfigurationTextBox(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = stringResource(Res.string.username),
                    )
                    ConfigurationTextBox(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = stringResource(Res.string.password),
                        visualTransformation =
                            if (uiState.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailing = {
                            IconButton(
                                onClick = {
                                    viewModel.onPasswordVisibilityChanged(!uiState.passwordVisible)
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.RemoveRedEye,
                                    contentDescription =
                                        stringResource(
                                            if (uiState.passwordVisible) Res.string.hide_password
                                            else Res.string.show_password
                                        ),
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default,
                    )
                }
            }
        }
    }
}

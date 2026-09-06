package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Filter1
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.docs_wildcards
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.general
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.learn_more
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.trusted_bssid
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.trusted_wifi_names
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_mapping
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_mapping_description
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.use_wildcards
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wifi_rules
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wifi_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wildcard_bssid_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wildcard_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wildcard_wifi_desc
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.TunnelNetwork
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.components.NetworkRuleInput
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AutoTunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun WifiSettingsScreen(viewModel: AutoTunnelViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()
    var currentSsidText by rememberSaveable { mutableStateOf("") }
    var currentBssidText by rememberSaveable { mutableStateOf("") }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    val wildcardEnabled = uiState.autoTunnelSettings.isWildcardsEnabled
    val ssidHint = viewModel.ssidHints.firstOrNull().orEmpty()
    val bssidHint = viewModel.bssidHints.firstOrNull().orEmpty()

    NestedSettingsScaffold(title = stringResource(Res.string.wifi_settings)) { padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.general),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Filter1, contentDescription = null) },
                    title = stringResource(Res.string.use_wildcards),
                    description = {
                        val descriptionText = stringResource(Res.string.wildcard_desc)
                        val learnMoreText = stringResource(Res.string.learn_more)
                        val url = stringResource(Res.string.docs_wildcards)
                        DescriptionText(
                            buildAnnotatedString {
                                append(descriptionText)
                                append(" ")
                                val start = length
                                append(learnMoreText)
                                addLink(LinkAnnotation.Url(url), start, length)
                            }
                        )
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = wildcardEnabled,
                            onClick = { viewModel.setWildcardsEnabled(it) },
                        )
                    },
                    onClick = { viewModel.setWildcardsEnabled(!wildcardEnabled) },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.wifi_rules),
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
                NetworkRuleInput(
                    inputTitle = stringResource(Res.string.trusted_wifi_names),
                    placeholder = ssidHint,
                    rules = uiState.autoTunnelSettings.trustedNetworkSsids,
                    onDelete = { viewModel.removeTrustedNetworkName(it) },
                    currentText = currentSsidText,
                    onValueChange = { currentSsidText = it },
                    onSave = { ssid ->
                        viewModel.saveTrustedNetworkName(ssid)
                        currentSsidText = ""
                    },
                    supportingContent = {
                        if (wildcardEnabled) {
                            DescriptionText(stringResource(Res.string.wildcard_wifi_desc))
                        }
                    },
                )
                NetworkRuleInput(
                    inputTitle = stringResource(Res.string.trusted_bssid),
                    placeholder = bssidHint,
                    rules = uiState.autoTunnelSettings.trustedNetworkBssids,
                    onDelete = { viewModel.removeTrustedBssid(it) },
                    currentText = currentBssidText,
                    onValueChange = { currentBssidText = it },
                    onSave = { bssid ->
                        viewModel.saveTrustedBssid(bssid)
                        currentBssidText = ""
                    },
                    supportingContent = {
                        if (wildcardEnabled) {
                            DescriptionText(stringResource(Res.string.wildcard_bssid_desc))
                        }
                    },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Map, contentDescription = null) },
                    title = stringResource(Res.string.tunnel_mapping),
                    description = {
                        DescriptionText(stringResource(Res.string.tunnel_mapping_description))
                    },
                    onClick = { navController.push(Route.PreferredTunnel(TunnelNetwork.WIFI)) },
                )
            }
        }
    }
}

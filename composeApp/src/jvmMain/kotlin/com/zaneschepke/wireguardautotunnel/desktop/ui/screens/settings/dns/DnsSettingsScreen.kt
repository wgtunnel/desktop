package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.dns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.constants.TunnelDns
import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.current_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_endpoint_hint
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_endpoint_label
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.domain_suffixes
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.peer_resolution
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.protocol
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.resolution_method
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.sdk
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_suffix_target
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.transit_dns_policy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.transit_dns_policy_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_dns
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_dns_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.use_tunnel_dns_servers
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dropdown.LabeledDropdown
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.OptionPickerMenu
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.PickerOption
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.util.asDescription
import com.zaneschepke.wireguardautotunnel.desktop.util.asIcon
import com.zaneschepke.wireguardautotunnel.desktop.util.asLabel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.DnsViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun DnsSettingsScreen(viewModel: DnsViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()
    var showModeMenu by rememberSaveable { mutableStateOf(false) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(
        title = stringResource(Res.string.dns_settings),
        showSave = uiState.isDirty,
        onSave = viewModel::save,
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier =
                Modifier.padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.peer_resolution),
                    Modifier.padding(horizontal = 16.dp),
                )
                LabeledDropdown(
                    title = stringResource(Res.string.resolution_method),
                    leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                    currentValue = uiState.draft.bootstrapDnsProtocol,
                    onSelected = { selected ->
                        selected?.let { viewModel.setBootstrapDnsProtocol(it) }
                    },
                    options = BootstrapDnsProtocol.entries,
                    optionToString = { (it ?: BootstrapDnsProtocol.SYSTEM).asLabel() },
                )
                AnimatedVisibility(
                    uiState.draft.bootstrapDnsProtocol != BootstrapDnsProtocol.SYSTEM
                ) {
                    ConfigurationTextBox(
                        modifier =
                            Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth(),
                        hint = stringResource(Res.string.dns_endpoint_hint),
                        label = stringResource(Res.string.dns_endpoint_label),
                        value = uiState.draft.bootstrapDnsEndpoint.orEmpty(),
                        onValueChange = viewModel::setBootstrapDnsEndpoint,
                    )
                }
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.tunnel_dns),
                    Modifier.padding(horizontal = 16.dp),
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
                            IconButton(onClick = { showModeMenu = true }, modifier = modifier) {
                                Icon(
                                    Icons.Outlined.ExpandMore,
                                    contentDescription = stringResource(Res.string.select),
                                )
                            }
                        },
                        title = stringResource(Res.string.tunnel_dns_mode),
                        description = {
                            DescriptionText(
                                stringResource(
                                    Res.string.current_template,
                                    uiState.draft.tunnelDnsMode.asLabel(),
                                )
                            )
                        },
                        onClick = { showModeMenu = true },
                    )
                    OptionPickerMenu(
                        expanded = showModeMenu,
                        onDismiss = { showModeMenu = false },
                        options =
                            TunnelDnsMode.entries.map { mode ->
                                PickerOption(
                                    leadingIcon = mode.asIcon(),
                                    label = mode.asLabel(),
                                    description = mode.asDescription(),
                                    selected = uiState.draft.tunnelDnsMode == mode,
                                    onClick = {
                                        showModeMenu = false
                                        viewModel.setTunnelDnsMode(mode)
                                    },
                                )
                            },
                    )
                }
                AnimatedVisibility(
                    uiState.draft.tunnelDnsMode in
                        setOf(TunnelDnsMode.Encrypted, TunnelDnsMode.Split, TunnelDnsMode.AllLocal)
                ) {
                    Column {
                        val isSplitMode = uiState.draft.tunnelDnsMode.isSplitMode()
                        val showCustomServer =
                            !isSplitMode ||
                                !(uiState.draft.useTunnelDnsServersInSplit &&
                                    uiState.draft.tunnelDnsProtocol == TunnelDnsProtocol.Plain)
                        if (uiState.draft.tunnelDnsMode != TunnelDnsMode.AllLocal) {
                            LabeledDropdown(
                                title = stringResource(Res.string.protocol),
                                leading = {
                                    Icon(Icons.Outlined.Router, contentDescription = null)
                                },
                                currentValue = uiState.draft.tunnelDnsProtocol,
                                onSelected = { selected ->
                                    selected?.let { viewModel.setTunnelDnsProtocol(it) }
                                },
                                options =
                                    if (isSplitMode) TunnelDnsProtocol.entries
                                    else
                                        TunnelDnsProtocol.entries.filter {
                                            it != TunnelDnsProtocol.Plain
                                        },
                                optionToString = { (it ?: TunnelDnsProtocol.Doh).asLabel() },
                            )
                        }
                        if (
                            isSplitMode &&
                                uiState.draft.tunnelDnsProtocol == TunnelDnsProtocol.Plain
                        ) {
                            SurfaceRow(
                                leading = { Icon(Icons.Outlined.Route, null) },
                                title = stringResource(Res.string.use_tunnel_dns_servers),
                                trailing = {
                                    ThemedSwitch(
                                        checked = uiState.draft.useTunnelDnsServersInSplit,
                                        onClick = viewModel::setUseTunnelDnsServersInSplit,
                                    )
                                },
                                onClick = {
                                    viewModel.setUseTunnelDnsServersInSplit(
                                        !uiState.draft.useTunnelDnsServersInSplit
                                    )
                                },
                            )
                        }
                        if (
                            showCustomServer &&
                                uiState.draft.tunnelDnsMode != TunnelDnsMode.AllLocal
                        ) {
                            ConfigurationTextBox(
                                modifier =
                                    Modifier.padding(horizontal = 16.dp)
                                        .padding(top = 8.dp)
                                        .fillMaxWidth(),
                                hint = stringResource(Res.string.dns_endpoint_hint),
                                label = stringResource(Res.string.dns_endpoint_label),
                                value = uiState.draft.tunnelDnsEndpoint.orEmpty(),
                                onValueChange = viewModel::setTunnelDnsEndpoint,
                            )
                        }
                        AnimatedVisibility(isSplitMode) {
                            Column {
                                ConfigurationTextBox(
                                    modifier =
                                        Modifier.padding(horizontal = 16.dp)
                                            .padding(top = 8.dp)
                                            .fillMaxWidth(),
                                    hint = TunnelDns.DEFAULT_SPLIT_SUFFIXES.joinToString(),
                                    label = stringResource(Res.string.domain_suffixes),
                                    value = uiState.draft.localSuffixes.orEmpty(),
                                    onValueChange = viewModel::setLocalSuffixes,
                                )
                                LabeledDropdown(
                                    title = stringResource(Res.string.split_suffix_target),
                                    leading = {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.AltRoute,
                                            contentDescription = null,
                                        )
                                    },
                                    currentValue = uiState.draft.splitSuffixTarget,
                                    onSelected = { selected ->
                                        selected?.let { viewModel.setSplitSuffixTarget(it) }
                                    },
                                    options = SplitDnsSuffixTarget.entries,
                                    optionToString = {
                                        (it ?: SplitDnsSuffixTarget.System).asLabel()
                                    },
                                )
                                DescriptionText(
                                    uiState.draft.splitSuffixTarget.asDescription(),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                        LabeledDropdown(
                            title = stringResource(Res.string.transit_dns_policy),
                            leading = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                            currentValue = uiState.draft.transitDnsPolicy,
                            onSelected = { selected ->
                                selected?.let { viewModel.setForeignDnsPolicy(it) }
                            },
                            options = TransitDnsPolicy.entries,
                            optionToString = { (it ?: TransitDnsPolicy.Redirect).asLabel() },
                        )
                        DescriptionText(
                            stringResource(Res.string.transit_dns_policy_desc),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

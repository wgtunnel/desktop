package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.lockdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.allow_lan_traffic
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bypass_lan_for_kill_switch
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.configuration
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.lockdown_settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.protect_on_startup
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.requires_daemon_running
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.DisabledReasonTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LockdownSettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(title = stringResource(Res.string.lockdown_settings)) { padding ->
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
                    leading = { Icon(Icons.Outlined.Lan, contentDescription = null) },
                    title = stringResource(Res.string.allow_lan_traffic),
                    description = {
                        DescriptionText(stringResource(Res.string.bypass_lan_for_kill_switch))
                    },
                    enabled = uiState.daemonConnected,
                    trailing = {
                        DisabledReasonTooltip(
                            enabled = uiState.daemonConnected,
                            reason = stringResource(Res.string.requires_daemon_running),
                        ) {
                            ThemedSwitch(
                                checked = uiState.lockdown.bypassLan,
                                enabled = uiState.daemonConnected,
                                onClick = { viewModel.onBypassLan(it) },
                            )
                        }
                    },
                    onClick = { viewModel.onBypassLan(!uiState.lockdown.bypassLan) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                    title = stringResource(Res.string.protect_on_startup),
                    enabled = uiState.lockdownEnabled && uiState.daemonConnected,
                    trailing = {
                        DisabledReasonTooltip(
                            enabled = uiState.daemonConnected,
                            reason = stringResource(Res.string.requires_daemon_running),
                        ) {
                            ThemedSwitch(
                                checked = uiState.lockdown.restoreOnBoot,
                                enabled = uiState.lockdownEnabled && uiState.daemonConnected,
                                onClick = { viewModel.onRestoreKillSwitchOnBoot(it) },
                            )
                        }
                    },
                    onClick = {
                        viewModel.onRestoreKillSwitchOnBoot(!uiState.lockdown.restoreOnBoot)
                    },
                )
            }
        }
    }
}

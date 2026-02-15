package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.appearance
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.general
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.lockdown
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.settings
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.LockdownIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current

    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> {
                toaster.show(Toast(sideEffect.message, sideEffect.type))
            }
        }
    }

    if (!uiState.isLoaded) return

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings)) }) }) {
        padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxSize().padding(padding),
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                GroupLabel(
                    stringResource(Res.string.lockdown),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                    title = "Enable lockdown mode",
                    onClick = {
                        viewModel.onLockdownAction(
                            LockdownIntent.ToggleMaster(!uiState.lockdownEnabled)
                        )
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.lockdownEnabled,
                            onClick = {
                                viewModel.onLockdownAction(LockdownIntent.ToggleMaster(it))
                            },
                        )
                    },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                    title = "Protect on system startup",
                    onClick = {
                        viewModel.onLockdownAction(
                            LockdownIntent.TogglePersist(!uiState.lockdownRestoreOnBootEnabled)
                        )
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.lockdownRestoreOnBootEnabled,
                            onClick = {
                                viewModel.onLockdownAction(LockdownIntent.TogglePersist(it))
                            },
                        )
                    },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Default.Lan, contentDescription = null) },
                    title = "Allow local network access",
                    onClick = {
                        viewModel.onLockdownAction(
                            LockdownIntent.ToggleBypassLan(!uiState.lockdownRestoreOnBootEnabled)
                        )
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.lockdownBypassEnabled,
                            onClick = {
                                viewModel.onLockdownAction(LockdownIntent.ToggleBypassLan(it))
                            },
                        )
                    },
                )
            }
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                GroupLabel(
                    stringResource(Res.string.tunnel),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                    title = "Restore tunnel on system startup",
                    onClick = {
                        viewModel.onRestoreTunnelOnBoot(!uiState.tunnelRestoreOnBootEnabled)
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.tunnelRestoreOnBootEnabled,
                            onClick = { viewModel.onRestoreTunnelOnBoot(it) },
                        )
                    },
                )
            }
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                GroupLabel(
                    stringResource(Res.string.general),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = {
                        Icon(Icons.AutoMirrored.Outlined.ViewQuilt, contentDescription = null)
                    },
                    title = stringResource(Res.string.appearance),
                    onClick = { navController.push(Route.Appearance) },
                )
            }
        }
    }
}

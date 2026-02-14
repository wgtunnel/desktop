package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.back
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.save
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components.ConfigEditor
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(viewModel: TunnelViewModel) {
    val toaster = LocalToaster.current
    val navController = LocalNavController.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    if (uiState.isDirty) {
                        IconButton(onClick = viewModel::saveChanges) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = stringResource(Res.string.save),
                            )
                        }
                    }
                },
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Coming soon!", style = MaterialTheme.typography.bodyLarge)
            }

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                var selectedTabIndex by remember {
                    val tab = if (uiState.activeConfig != null) 1 else 0
                    mutableIntStateOf(tab)
                }
                val isTunnelActive = uiState.tunnelState != null

                SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Edit tunnel") },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        enabled = isTunnelActive,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                "Live tunnel",
                                color =
                                    if (uiState.activeConfig != null)
                                        MaterialTheme.colorScheme.onSurface
                                    else Color.Gray,
                            )
                        },
                    )
                }

                when (selectedTabIndex) {
                    0 ->
                        ConfigEditor(
                            rawConfig = uiState.editedConfig.quickConfig,
                            isEditable = true,
                            onConfigChange = { viewModel.onConfigUpdate(it) },
                        )
                    1 -> {
                        key(uiState.activeConfig) {
                            ConfigEditor(
                                rawConfig = uiState.activeConfig?.asQuickString() ?: "No data",
                                isEditable = false,
                                onConfigChange = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

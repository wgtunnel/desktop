package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnels
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.TunnelList
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.util.FileUtils
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelsViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelsScreen(viewModel: TunnelsViewModel = koinViewModel()) {

    val uiState by viewModel.collectAsState()

    var pendingDeleteIntent by remember { mutableStateOf<DeleteIntent?>(null) }

    val toaster = LocalToaster.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> {
                toaster.show(Toast(sideEffect.message, sideEffect.type))
            }
        }
    }

    pendingDeleteIntent?.let { intent ->
        InfoDialog(
            onAttest = {
                viewModel.onDelete(intent)
                pendingDeleteIntent = null
            },
            onDismiss = { pendingDeleteIntent = null },
            title = "Delete tunnel",
            confirmText = "Yes",
            body = {
                when (intent) {
                    DeleteIntent.Selected -> {
                        Text("Are you sure you want to delete the selected tunnels?")
                    }
                    is DeleteIntent.Tunnel -> {
                        Text("Are you sure you want to delete ${intent.tunnel.name}?")
                    }
                }
            },
        )
    }

    if (!uiState.isLoaded) return

    val scope = rememberCoroutineScope()
    val pickerLauncher =
        rememberFilePickerLauncher(mode = FileKitMode.Single) { file: PlatformFile? ->
            file?.let {
                when (it.extension) {
                    FileUtils.CONF_FILE_EXTENSION -> {
                        scope.launch {
                            val text = it.readString()
                            viewModel.onConfImport(text, it.file.nameWithoutExtension)
                        }
                    }
                    FileUtils.ZIP_FILE_EXTENSION -> {
                        scope.launch {
                            val bytes = it.readBytes()
                            val configMap = FileUtils.readConfigsFromZip(bytes)
                            viewModel.onMultiConfImport(configMap)
                        }
                    }
                    else -> {
                        toaster.show(
                            Toast(
                                type = ToastType.Warning,
                                message = "Only '.conf' or '.zip' files supported",
                            )
                        )
                    }
                }
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tunnels)) },
                actions = {
                    if (!uiState.isSelectionMode) {
                        CustomTooltip(text = "Add a tunnel") {
                            IconButton(onClick = { pickerLauncher.launch() }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Add a tunnel")
                            }
                        }
                        return@TopAppBar
                    }
                    Row {
                        CustomTooltip(text = "Select all") {
                            IconButton(onClick = viewModel::onSelectAll) {
                                Icon(
                                    Icons.Outlined.SelectAll,
                                    contentDescription = "Delete selected",
                                )
                            }
                        }
                        CustomTooltip(text = "Delete selected") {
                            IconButton(onClick = { pendingDeleteIntent = DeleteIntent.Selected }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                            }
                        }
                        CustomTooltip(text = "Export selected") {
                            IconButton(
                                onClick = { viewModel.onExportIntent(ExportIntent.Selected) }
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = "Export selected",
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(uiState.isSelectionMode) {
                        if (uiState.isSelectionMode) {
                            detectTapGestures(onPress = { viewModel.onClearSelectionMode() })
                        }
                    }
        )
        TunnelList(
            uiState = uiState,
            startTunnel = viewModel::onStartTunnel,
            stopTunnel = viewModel::onStopTunnel,
            viewModel::onItemsReordered,
            viewModel::onPersistReorder,
            viewModel::onSelectTunnel,
            viewModel::onDeselectTunnel,
            viewModel::onClearSelectionMode,
            { intent -> pendingDeleteIntent = intent },
            viewModel::onExportIntent,
        )
    }
}

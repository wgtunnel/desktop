package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileOpen
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.add_a_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group_and_tunnels
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group_and_tunnels_message
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group_message
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_selected_tunnels
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_tunnel_message
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.import_from_file
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.move_to_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.add_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.rename_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.failed_to_read_conf_file
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.failed_to_read_zip_archive
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.only_conf_or_zip_supported
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select_all
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.cancel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnels
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.yes
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.OptionPickerMenu
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu.PickerOption
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.GroupNameDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.MoveToGroupDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.TunnelList
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.util.FileUtils
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelsViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.Dispatchers
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
    var showAddMenu by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
    var renameGroup by remember { mutableStateOf<TunnelGroup?>(null) }
    var moveTargets by remember { mutableStateOf<List<TunnelConfig>?>(null) }
    var newGroupTargets by remember { mutableStateOf<List<TunnelConfig>?>(null) }

    val toaster = LocalToaster.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> {
                toaster.show(Toast(sideEffect.message, sideEffect.type))
            }
            else -> Unit
        }
    }

    pendingDeleteIntent?.let { intent ->
        InfoDialog(
            onAttest = {
                viewModel.onDelete(intent)
                pendingDeleteIntent = null
            },
            onDismiss = { pendingDeleteIntent = null },
            title =
                when (intent) {
                    is DeleteIntent.Group -> stringResource(Res.string.delete_group)
                    is DeleteIntent.GroupAndTunnels ->
                        stringResource(Res.string.delete_group_and_tunnels)
                    else -> stringResource(Res.string.delete_tunnel)
                },
            confirmText = stringResource(Res.string.yes),
            body = {
                when (intent) {
                    DeleteIntent.Selected -> {
                        Text(stringResource(Res.string.delete_selected_tunnels))
                    }
                    is DeleteIntent.Tunnel -> {
                        Text(stringResource(Res.string.delete_tunnel_message))
                    }
                    is DeleteIntent.Group -> {
                        Text(stringResource(Res.string.delete_group_message))
                    }
                    is DeleteIntent.GroupAndTunnels -> {
                        Text(stringResource(Res.string.delete_group_and_tunnels_message))
                    }
                }
            },
        )
    }

    if (!uiState.isLoaded) return

    val scope = rememberCoroutineScope()
    val failedToReadConfMessage = stringResource(Res.string.failed_to_read_conf_file)
    val failedToReadZipMessage = stringResource(Res.string.failed_to_read_zip_archive)
    val unsupportedFileTypeMessage = stringResource(Res.string.only_conf_or_zip_supported)
    val pickerLauncher =
        rememberFilePickerLauncher(mode = FileKitMode.Single) { platformFile: PlatformFile? ->
            platformFile?.let { file ->
                val ext = file.extension.lowercase()

                scope.launch(Dispatchers.Main.immediate) {
                    when (ext) {
                        FileUtils.CONF_FILE_EXTENSION -> {
                            runCatching {
                                val text = file.readString()
                                viewModel.onConfImport(text, file.nameWithoutExtension)
                            }
                                .onFailure {
                                    toaster.show(Toast(ToastType.Error, failedToReadConfMessage))
                                }
                        }
                        FileUtils.ZIP_FILE_EXTENSION -> {
                            runCatching {
                                val bytes = file.readBytes()
                                val configMap = FileUtils.readConfigsFromZip(bytes)
                                viewModel.onMultiConfImport(configMap)
                            }
                                .onFailure {
                                    toaster.show(Toast(ToastType.Error, failedToReadZipMessage))
                                }
                        }
                        else -> {
                            toaster.show(
                                Toast(
                                    type = ToastType.Warning,
                                    message = unsupportedFileTypeMessage,
                                )
                            )
                        }
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
                        Box {
                            CustomTooltip(text = stringResource(Res.string.add_a_tunnel)) {
                                IconButton(onClick = { showAddMenu = true }) {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription =
                                            stringResource(Res.string.add_a_tunnel),
                                    )
                                }
                            }
                            OptionPickerMenu(
                                expanded = showAddMenu,
                                onDismiss = { showAddMenu = false },
                                options =
                                    listOf(
                                        PickerOption(
                                            leadingIcon = Icons.Outlined.FileOpen,
                                            label = stringResource(Res.string.import_from_file),
                                            onClick = {
                                                showAddMenu = false
                                                pickerLauncher.launch()
                                            },
                                        ),
                                        PickerOption(
                                            leadingIcon = Icons.Outlined.CreateNewFolder,
                                            label = stringResource(Res.string.add_group),
                                            onClick = {
                                                showAddMenu = false
                                                showNewGroup = true
                                            },
                                        ),
                                    ),
                            )
                        }
                        return@TopAppBar
                    }
                    Row {
                        CustomTooltip(text = stringResource(Res.string.move_to_group)) {
                            IconButton(onClick = { moveTargets = uiState.selectedTunnels }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.DriveFileMove,
                                    contentDescription = stringResource(Res.string.move_to_group),
                                )
                            }
                        }
                        CustomTooltip(text = stringResource(Res.string.select_all)) {
                            IconButton(onClick = viewModel::onSelectAll) {
                                Icon(
                                    Icons.Outlined.SelectAll,
                                    contentDescription = stringResource(Res.string.select_all),
                                )
                            }
                        }
                        CustomTooltip(text = stringResource(Res.string.delete_selected)) {
                            IconButton(onClick = { pendingDeleteIntent = DeleteIntent.Selected }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(Res.string.delete_selected),
                                )
                            }
                        }
                        CustomTooltip(text = stringResource(Res.string.export_selected)) {
                            IconButton(
                                onClick = { viewModel.onExportIntent(ExportIntent.Selected) }
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = stringResource(Res.string.export_selected),
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
        ) {
            TunnelList(
                uiState = uiState,
                startTunnel = viewModel::onStartTunnel,
                stopTunnel = viewModel::onStopTunnel,
                viewModel::onItemsReordered,
                viewModel::onPersistReorder,
                viewModel::onSelectTunnel,
                viewModel::onDeselectTunnel,
                viewModel::onSelectTunnels,
                viewModel::onClearSelectionMode,
                { intent -> pendingDeleteIntent = intent },
                viewModel::onExportIntent,
                onToggleGroup = viewModel::onToggleGroupExpanded,
                onRenameGroup = { renameGroup = it },
                onMoveToGroup = { moveTargets = it },
                onUngroup = viewModel::onUngroupTunnels,
            )
        }
    }

    if (showNewGroup) {
        GroupNameDialog(
            title = stringResource(Res.string.add_group),
            initialName = "",
            onDismiss = { showNewGroup = false },
            onConfirm = { name ->
                viewModel.onCreateGroup(name)
                showNewGroup = false
            },
        )
    }

    renameGroup?.let { group ->
        GroupNameDialog(
            title = stringResource(Res.string.rename_group),
            initialName = group.name,
            onDismiss = { renameGroup = null },
            onConfirm = { name ->
                viewModel.onRenameGroup(group, name)
                renameGroup = null
            },
        )
    }

    moveTargets?.let { targets ->
        MoveToGroupDialog(
            groups = uiState.groups,
            onDismiss = { moveTargets = null },
            onSelect = { group ->
                viewModel.onMoveToGroup(group.id, targets)
                moveTargets = null
            },
            onNewGroup = {
                newGroupTargets = targets
                moveTargets = null
            },
        )
    }

    newGroupTargets?.let { targets ->
        GroupNameDialog(
            title = stringResource(Res.string.add_group),
            initialName = "",
            onDismiss = { newGroupTargets = null },
            onConfirm = { name ->
                viewModel.onCreateGroupAndMove(name, targets)
                newGroupTargets = null
            },
        )
    }
}

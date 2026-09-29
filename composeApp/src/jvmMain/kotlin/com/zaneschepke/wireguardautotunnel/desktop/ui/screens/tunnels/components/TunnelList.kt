package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.group_active_count
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.collapse_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_group_and_tunnels
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.details
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.exit_selection_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.expand_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.group_tunnel_count
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.move_to_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_tunnels_click_add
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.rename_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select_tunnels
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ungroup
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollbarThickness
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.appScrollbarStyle
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.RequiresDaemonTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelListRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
import dev.nucleusframework.application.contextmenu.NucleusContextMenuItem
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun TunnelList(
    uiState: TunnelsUiState,
    startTunnel: (id: Long) -> Unit,
    stopTunnel: (id: Long) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReorderCompleted: () -> Unit,
    onSelected: (conf: TunnelConfig) -> Unit,
    onDeselected: (conf: TunnelConfig) -> Unit,
    onSelectRange: (List<TunnelConfig>) -> Unit,
    onExitSelectionMode: () -> Unit,
    onDelete: (intent: DeleteIntent) -> Unit,
    onExport: (intent: ExportIntent) -> Unit,
    onToggleGroup: (TunnelGroup) -> Unit,
    onRenameGroup: (TunnelGroup) -> Unit,
    onMoveToGroup: (List<TunnelConfig>) -> Unit,
    onUngroup: (List<TunnelConfig>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val lazyListState = rememberLazyListState()
    val rows = uiState.rows
    val visibleTunnels = rows.filterIsInstance<TunnelListRow.TunnelRow>().map { it.tunnel }
    val windowInfo = LocalWindowInfo.current
    // Where a Shift-click range starts from, the last tunnel picked by a plain or Ctrl-click
    var anchorId by remember { mutableStateOf<Long?>(null) }

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.index, to.index)
        }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            onReorderCompleted()
        }
    }

    val detailsLabel = stringResource(Res.string.details)
    val deleteLabel = stringResource(Res.string.delete)
    val exportLabel = stringResource(Res.string.export)
    val selectLabel = stringResource(Res.string.select_tunnels)
    val deleteSelectedLabel = stringResource(Res.string.delete_selected)
    val exportSelectedLabel = stringResource(Res.string.export_selected)
    val exitSelectionLabel = stringResource(Res.string.exit_selection_mode)
    val renameLabel = stringResource(Res.string.rename_group)
    val exportGroupLabel = stringResource(Res.string.export_group)
    val deleteGroupLabel = stringResource(Res.string.delete_group)
    val deleteGroupAndTunnelsLabel = stringResource(Res.string.delete_group_and_tunnels)
    val moveToGroupLabel = stringResource(Res.string.move_to_group)
    val ungroupLabel = stringResource(Res.string.ungroup)

    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .onKeyEvent {
                    if (it.key == Key.Escape && uiState.isSelectionMode) {
                        onExitSelectionMode()
                        true
                    } else false
                }
                .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(end = ScrollbarThickness),
        ) {
            if (uiState.tunnelItems.isEmpty() && uiState.groups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(Res.string.no_tunnels_click_add))
                    }
                }
                return@LazyColumn
            }

            items(rows, key = { it.key }) { row ->
                ReorderableItem(reorderableState, key = row.key) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                    when (row) {
                        is TunnelListRow.GroupHeader -> {
                            val draggable = !uiState.isSelectionMode && !row.visibleExpanded
                            ContextMenuArea(
                                items = {
                                    buildList {
                                        add(
                                            NucleusContextMenuItem(renameLabel) {
                                                onRenameGroup(row.group)
                                            }
                                        )
                                        if (row.childCount > 0) {
                                            add(
                                                NucleusContextMenuItem(exportGroupLabel) {
                                                    onExport(ExportIntent.Group(row.group))
                                                }
                                            )
                                        }
                                        add(
                                            NucleusContextMenuItem(deleteGroupLabel) {
                                                onDelete(DeleteIntent.Group(row.group))
                                            }
                                        )
                                        if (row.childCount > 0) {
                                            add(
                                                NucleusContextMenuItem(deleteGroupAndTunnelsLabel) {
                                                    onDelete(DeleteIntent.GroupAndTunnels(row.group))
                                                }
                                            )
                                        }
                                    }
                                }
                            ) {
                                SurfaceRow(
                                    title = row.group.name,
                                    modifier =
                                        Modifier.shadow(elevation)
                                            .animateItem()
                                            .then(
                                                if (draggable) Modifier.draggableHandle()
                                                else Modifier
                                            )
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .onKeyEvent {
                                                if (it.key == Key.F2) {
                                                    onRenameGroup(row.group)
                                                    true
                                                } else false
                                            }
                                            .then(if (isDragging) Modifier.zIndex(1f) else Modifier),
                                    onClick = { onToggleGroup(row.group) },
                                    description =
                                        if (row.childCount > 0) {
                                            {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        pluralStringResource(
                                                            Res.plurals.group_tunnel_count,
                                                            row.childCount,
                                                            row.childCount,
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                    if (row.activeCount > 0) {
                                                        Text(
                                                            " · " +
                                                                stringResource(
                                                                    Res.string.group_active_count,
                                                                    row.activeCount,
                                                                ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                }
                                            }
                                        } else null,
                                    leading = {
                                        Icon(
                                            if (row.visibleExpanded) Icons.Outlined.FolderOpen
                                            else Icons.Outlined.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    trailing = {
                                        Icon(
                                            if (row.visibleExpanded) Icons.Rounded.ExpandLess
                                            else Icons.Rounded.ExpandMore,
                                            contentDescription =
                                                stringResource(
                                                    if (row.visibleExpanded)
                                                        Res.string.collapse_group
                                                    else Res.string.expand_group
                                                ),
                                        )
                                    },
                                )
                            }
                        }
                        is TunnelListRow.TunnelRow -> {
                            val item =
                                uiState.tunnelItems.first { it.config.id == row.tunnel.id }
                            val isSelected = uiState.selectedTunnels.contains(item.config)
                            ContextMenuArea(
                                items = {
                                    buildList {
                                        if (!uiState.isSelectionMode) {
                                            add(
                                                NucleusContextMenuItem(detailsLabel) {
                                                    navController.push(Route.Tunnel(item.config.id))
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(deleteLabel) {
                                                    onDelete(DeleteIntent.Tunnel(item.config))
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(exportLabel) {
                                                    onExport(ExportIntent.Tunnel(item.config))
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(moveToGroupLabel) {
                                                    onMoveToGroup(listOf(item.config))
                                                }
                                            )
                                            if (item.config.groupId != null) {
                                                add(
                                                    NucleusContextMenuItem(ungroupLabel) {
                                                        onUngroup(listOf(item.config))
                                                    }
                                                )
                                            }
                                            add(
                                                NucleusContextMenuItem(selectLabel) {
                                                    onSelected(item.config)
                                                }
                                            )
                                        } else {
                                            add(
                                                NucleusContextMenuItem(deleteSelectedLabel) {
                                                    onDelete(DeleteIntent.Selected)
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(exportSelectedLabel) {
                                                    onExport(ExportIntent.Selected)
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(moveToGroupLabel) {
                                                    onMoveToGroup(uiState.selectedTunnels)
                                                }
                                            )
                                            add(
                                                NucleusContextMenuItem(exitSelectionLabel) {
                                                    onExitSelectionMode()
                                                }
                                            )
                                        }
                                    }
                                }
                            ) {
                                SurfaceRow(
                                    title = item.config.name,
                                    modifier =
                                        Modifier.shadow(elevation)
                                            .animateItem()
                                            .then(
                                                if (row.grouped) Modifier.padding(start = 24.dp)
                                                else Modifier
                                            )
                                            .then(
                                                if (!uiState.isSelectionMode)
                                                    Modifier.draggableHandle()
                                                else Modifier
                                            )
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .then(if (isDragging) Modifier.zIndex(1f) else Modifier),
                                    onClick = {
                                        val keys = windowInfo.keyboardModifiers
                                        val additive = keys.isCtrlPressed || keys.isMetaPressed
                                        when {
                                            keys.isShiftPressed && anchorId != null -> {
                                                val ids = visibleTunnels.map { it.id }
                                                val from = ids.indexOf(anchorId)
                                                val to = ids.indexOf(item.config.id)
                                                if (from >= 0 && to >= 0) {
                                                    onSelectRange(
                                                        visibleTunnels.subList(
                                                            minOf(from, to),
                                                            maxOf(from, to) + 1,
                                                        )
                                                    )
                                                }
                                            }
                                            additive || uiState.isSelectionMode -> {
                                                anchorId = item.config.id
                                                if (isSelected) onDeselected(item.config)
                                                else onSelected(item.config)
                                            }
                                            else -> navController.push(Route.Tunnel(item.config.id))
                                        }
                                    },
                                    leading = {
                                        @Composable
                                        fun icon() {
                                            Icon(
                                                Icons.Rounded.Circle,
                                                contentDescription = null,
                                                tint = item.stateColor,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        val tooltipMessage = item.status?.state?.asTooltipMessage()
                                        if (!tooltipMessage.isNullOrBlank()) {
                                            CustomTooltip(text = tooltipMessage) { icon() }
                                        } else {
                                            icon()
                                        }
                                    },
                                    selected = isSelected,
                                    description = { TunnelStatisticsSection(status = item.status) },
                                    trailing = {
                                        if (!uiState.isSelectionMode) {
                                            RequiresDaemonTooltip(
                                                daemonConnected = uiState.hasBackendStatus
                                            ) {
                                                SwitchWithDivider(
                                                    checked = item.isRunning,
                                                    enabled = uiState.hasBackendStatus,
                                                    onClick = {
                                                        if (it) startTunnel(item.config.id)
                                                        else stopTunnel(item.config.id)
                                                    },
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = appScrollbarStyle(),
        )
    }
}

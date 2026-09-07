package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.details
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.exit_selection_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_tunnels_click_add
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select_tunnels
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
import dev.nucleusframework.application.contextmenu.NucleusContextMenuItem
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
    onExitSelectionMode: () -> Unit,
    onDelete: (intent: DeleteIntent) -> Unit,
    onExport: (intent: ExportIntent) -> Unit,
    onDaemonRequired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val lazyListState = rememberLazyListState()

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

    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .onKeyEvent {
                    if (it.key == Key.Escape && uiState.isSelectionMode) {
                        onExitSelectionMode()
                        true
                    } else false
                }
                .fillMaxSize(),
    ) {
        if (uiState.tunnelItems.isEmpty()) {
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

        items(uiState.tunnelItems, key = { it.config.id }) { item ->
            val isSelected = uiState.selectedTunnels.contains(item.config)
            ReorderableItem(reorderableState, key = item.config.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

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
                                add(NucleusContextMenuItem(selectLabel) { onSelected(item.config) })
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
                                    if (!uiState.isSelectionMode) Modifier.draggableHandle()
                                    else Modifier
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .then(if (isDragging) Modifier.zIndex(1f) else Modifier),
                        onClick = {
                            if (!uiState.isSelectionMode) {
                                navController.push(Route.Tunnel(item.config.id))
                            }
                        },
                        leading = {
                            val item = uiState.tunnelItems.first { it.config.id == item.config.id }
                            @Composable
                            fun icon() {
                                Icon(
                                    Icons.Rounded.Circle,
                                    contentDescription = null,
                                    tint = item.stateColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            if (item.tooltipMessage.isNotBlank()) {
                                CustomTooltip(text = item.tooltipMessage) { icon() }
                            } else {
                                icon()
                            }
                        },
                        selected = isSelected,
                        description = { TunnelStatisticsSection(status = item.status) },
                        trailing = {
                            if (!uiState.isSelectionMode) {
                                SwitchWithDivider(
                                    checked = item.isRunning,
                                    enabled = uiState.hasBackendStatus,
                                    onClick = {
                                        if (it) startTunnel(item.config.id)
                                        else stopTunnel(item.config.id)
                                    },
                                    onDisabledClick = onDaemonRequired,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

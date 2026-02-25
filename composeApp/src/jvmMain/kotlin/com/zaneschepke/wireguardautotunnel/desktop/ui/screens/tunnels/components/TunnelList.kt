package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SwitchWithDivider
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
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
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val lazyListState = rememberLazyListState()

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.index, to.index)
        }

    val tunnelIndicators by
        remember(uiState.tunnelStates, uiState.tunnels) {
            derivedStateOf {
                uiState.tunnels.associate { tunnel ->
                    val state =
                        uiState.tunnelStates.firstOrNull { it.id == tunnel.id }?.state
                            ?: TunnelState.UNKNOWN
                    tunnel.id to (state.asColor() to state.asTooltipMessage())
                }
            }
        }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            onReorderCompleted()
        }
    }

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
        if (uiState.tunnels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No tunnels added yet! Click the + symbol to add a tunnel.")
                }
            }
            return@LazyColumn
        }

        items(uiState.tunnels, key = { it.id }) { tunnel ->
            val isSelected = uiState.selectedTunnels.contains(tunnel)
            ReorderableItem(reorderableState, key = tunnel.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                ContextMenuArea(
                    items = {
                        buildList {
                            if (!uiState.isSelectionMode) {
                                add(
                                    ContextMenuItem("Details") {
                                        navController.push(Route.Tunnel(tunnel.id))
                                    }
                                )
                                add(
                                    ContextMenuItem("Delete") {
                                        onDelete(DeleteIntent.Tunnel(tunnel))
                                    }
                                )
                                add(
                                    ContextMenuItem("Export") {
                                        onExport(ExportIntent.Tunnel(tunnel))
                                    }
                                )
                                add(ContextMenuItem("Select") { onSelected(tunnel) })
                            } else {
                                add(
                                    ContextMenuItem("Delete selected") {
                                        onDelete(DeleteIntent.Selected)
                                    }
                                )
                                add(
                                    ContextMenuItem("Export selected") {
                                        onExport(ExportIntent.Selected)
                                    }
                                )
                                add(
                                    ContextMenuItem("Exit selection mode") { onExitSelectionMode() }
                                )
                            }
                        }
                    }
                ) {
                    SurfaceRow(
                        title = tunnel.name,
                        modifier =
                            Modifier.shadow(elevation)
                                .animateItem()
                                .then(
                                    if (!uiState.isSelectionMode) Modifier.draggableHandle()
                                    else Modifier
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .then(if (isDragging) Modifier.zIndex(1f) else Modifier)
                                .clickable(
                                    enabled = !uiState.isSelectionMode,
                                    onClick = { navController.push(Route.Tunnel(tunnel.id)) },
                                ),
                        leading = {
                            val tooltip = tunnelIndicators[tunnel.id]?.second
                            val indicatorColor = tunnelIndicators[tunnel.id]?.first
                            @Composable
                            fun icon() {
                                Icon(
                                    Icons.Rounded.Circle,
                                    contentDescription = null,
                                    tint = indicatorColor ?: TunnelState.UNKNOWN.asColor(),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            if (tooltip != null) {
                                CustomTooltip(text = tooltip) { icon() }
                            } else {
                                icon()
                            }
                        },
                        selected = isSelected,
                        trailing = {
                            if (!uiState.isSelectionMode) {
                                SwitchWithDivider(
                                    checked = tunnel.active,
                                    onClick = {
                                        if (it) startTunnel(tunnel.id) else stopTunnel(tunnel.id)
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

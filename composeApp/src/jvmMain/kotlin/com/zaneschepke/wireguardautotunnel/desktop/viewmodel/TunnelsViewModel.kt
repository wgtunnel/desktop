package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelGroupRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_cancelled
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.exported_to_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_tunnels_in_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_tunnels_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_not_found
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown_error
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelUiItem
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.moveDisplayedRows
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.nextChildPosition
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.nextRootPosition
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.ungroupKeepingOrder
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.uniqueDisplayName
import com.zaneschepke.wireguardautotunnel.desktop.util.FileUtils
import com.zaneschepke.wireguardautotunnel.desktop.util.asUserMessage
import com.zaneschepke.wireguardautotunnel.desktop.util.toConfigErrorMessage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class TunnelsViewModel(
    private val tunnelRepository: TunnelRepository,
    private val tunnelGroupRepository: TunnelGroupRepository,
    private val tunnelCoordinator: TunnelCoordinator,
    private val tunnelImportService: TunnelImportService,
    private val backendService: BackendService,
) : OrbitContainerHost<TunnelsUiState, TunnelsUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<TunnelsUiState, AppSideEffect>(TunnelsUiState()) {
            intent {
                tunnelRepository.userTunnelsFlow.collect { configs ->
                    reduce {
                        val currentItems = state.tunnelItems
                        val updatedItems = configs.map { config ->
                            val existingStatus =
                                currentItems.firstOrNull { it.config.id == config.id }?.status
                            TunnelUiItem(config = config, status = existingStatus)
                        }
                        state.copy(tunnelItems = updatedItems, isLoaded = true)
                    }
                }
            }

            intent {
                tunnelGroupRepository.flow.collect { groups ->
                    reduce { state.copy(groups = groups) }
                }
            }

            intent {
                backendService.statusFlow().collect { backendStatus ->
                    reduce {
                        val updatedItems =
                            state.tunnelItems.map { item ->
                                val newStatus =
                                    backendStatus.activeTunnels.firstOrNull {
                                        it.id == item.config.id
                                    }
                                item.copy(status = newStatus)
                            }
                        state.copy(tunnelItems = updatedItems, hasBackendStatus = true)
                    }
                }
            }
        }

    fun onItemsReordered(fromIndex: Int, toIndex: Int) = intent {
        val result =
            moveDisplayedRows(
                state.groups,
                state.tunnelItems.map { it.config },
                fromIndex,
                toIndex,
                state.rows,
            ) ?: return@intent
        val (groups, tunnels) = result
        val statusById = state.tunnelItems.associate { it.config.id to it.status }
        reduce {
            state.copy(
                groups = groups,
                tunnelItems = tunnels.map { config -> TunnelUiItem(config, statusById[config.id]) },
            )
        }
    }

    fun onPersistReorder() = intent {
        tunnelRepository.updateAll(state.tunnelItems.map { it.config })
        tunnelGroupRepository.saveAll(state.groups)
    }

    fun onStartTunnel(id: Long) = intent {
        val tunnel =
            tunnelRepository.getById(id)
                ?: run {
                    postSideEffect(
                        AppSideEffect.Toast(getString(Res.string.tunnel_not_found), ToastType.Error)
                    )
                    return@intent
                }
        tunnelCoordinator.startTunnel(tunnel).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onStopTunnel(id: Long) = intent {
        tunnelCoordinator.stopTunnel(id).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onSelectTunnel(tunnel: TunnelConfig) = intent {
        reduce {
            state.copy(selectedTunnels = state.selectedTunnels.plus(tunnel), isSelectionMode = true)
        }
    }

    fun onDeselectTunnel(tunnel: TunnelConfig) = intent {
        val selected = state.selectedTunnels.minus(tunnel)
        reduce { state.copy(selectedTunnels = selected, isSelectionMode = selected.isNotEmpty()) }
    }

    fun onClearSelectionMode() = intent {
        reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
    }

    fun onMultiConfImport(configMap: Map<String, String>) = intent {
        tunnelImportService.import(configMap).onFailure {
            postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
        }
    }

    fun onConfImport(quickString: String, name: String?) = intent {
        tunnelImportService.import(quickString, name).onFailure {
            postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
        }
    }

    fun onExportIntent(intent: ExportIntent) = intent {
        val (file, bytes) =
            when (intent) {
                ExportIntent.Selected -> {
                    if (state.selectedTunnels.isEmpty()) {
                        postSideEffect(
                            AppSideEffect.Toast(
                                getString(Res.string.no_tunnels_selected),
                                ToastType.Warning,
                            )
                        )
                        return@intent
                    }
                    val configMap = state.selectedTunnels.associate { it.name to it.quickConfig }
                    val zipBytes = FileUtils.createZipArchive(configMap)
                    FileKit.openFileSaver(
                        suggestedName = "tunnels",
                        defaultExtension = FileUtils.ZIP_FILE_EXTENSION,
                        directory = null,
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    ) to zipBytes
                }
                is ExportIntent.Tunnel -> {
                    FileKit.openFileSaver(
                        suggestedName = intent.tunnel.name,
                        defaultExtension = FileUtils.CONF_FILE_EXTENSION,
                        directory = null,
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    ) to intent.tunnel.quickConfig.toByteArray()
                }
                is ExportIntent.Group -> {
                    val children =
                        state.tunnelItems.map { it.config }.filter { it.groupId == intent.group.id }
                    if (children.isEmpty()) {
                        postSideEffect(
                            AppSideEffect.Toast(
                                getString(Res.string.no_tunnels_in_group),
                                ToastType.Warning,
                            )
                        )
                        return@intent
                    }
                    FileKit.openFileSaver(
                        suggestedName = intent.group.name,
                        defaultExtension = FileUtils.ZIP_FILE_EXTENSION,
                        directory = null,
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    ) to
                        FileUtils.createZipArchive(children.associate { it.name to it.quickConfig })
                }
            }

        try {
            if (file != null) {
                file.write(bytes)
                postSideEffect(
                    AppSideEffect.Toast(
                        getString(Res.string.exported_to_template, file.name),
                        ToastType.Success,
                    )
                )
            } else {
                postSideEffect(
                    AppSideEffect.Toast(getString(Res.string.export_cancelled), ToastType.Info)
                )
            }
        } catch (e: Exception) {
            postSideEffect(
                AppSideEffect.Toast(
                    getString(
                        Res.string.export_failed,
                        e.message ?: getString(Res.string.unknown_error),
                    ),
                    ToastType.Error,
                )
            )
        }
        reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
    }

    fun onSelectAll() = intent {
        reduce {
            state.copy(
                isSelectionMode = true,
                selectedTunnels = state.tunnelItems.map { it.config },
            )
        }
    }

    fun onDelete(intent: DeleteIntent) = intent {
        when (intent) {
            DeleteIntent.Selected -> {
                tunnelRepository.delete(state.selectedTunnels.map { it.id })
                reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
            }
            is DeleteIntent.Tunnel -> {
                tunnelRepository.delete(intent.tunnel.id)
            }
            is DeleteIntent.Group -> {
                val tunnels = state.tunnelItems.map { it.config }
                val (groups, updated) = ungroupKeepingOrder(state.groups, tunnels, intent.group.id)
                tunnelRepository.updateAll(updated)
                tunnelGroupRepository.saveAll(groups)
                tunnelGroupRepository.delete(intent.group.id)
            }
            is DeleteIntent.GroupAndTunnels -> {
                val childIds =
                    state.tunnelItems
                        .map { it.config }
                        .filter { it.groupId == intent.group.id }
                        .map { it.id }
                if (childIds.isNotEmpty()) tunnelRepository.delete(childIds)
                tunnelGroupRepository.delete(intent.group.id)
                val remaining = state.selectedTunnels.filter { it.id !in childIds }
                reduce {
                    state.copy(
                        selectedTunnels = remaining,
                        isSelectionMode = remaining.isNotEmpty(),
                    )
                }
            }
        }
    }

    fun onCreateGroup(name: String) = intent {
        val unique = uniqueDisplayName(name, state.groups.map { it.name }, "Group")
        tunnelGroupRepository.save(
            TunnelGroup(
                name = unique,
                position = nextRootPosition(state.groups, state.tunnelItems.map { it.config }),
                expanded = true,
            )
        )
    }

    fun onRenameGroup(group: TunnelGroup, name: String) = intent {
        val unique =
            uniqueDisplayName(
                name,
                state.groups.filter { it.id != group.id }.map { it.name },
                "Group",
            )
        tunnelGroupRepository.save(group.copy(name = unique))
    }

    fun onToggleGroupExpanded(group: TunnelGroup) = intent {
        tunnelGroupRepository.setExpanded(group.id, !group.expanded)
    }

    fun onCreateGroupAndMove(name: String, tunnels: List<TunnelConfig>) = intent {
        if (tunnels.isEmpty()) return@intent
        val unique = uniqueDisplayName(name, state.groups.map { it.name }, "Group")
        val groupId =
            tunnelGroupRepository.save(
                TunnelGroup(
                    name = unique,
                    position = nextRootPosition(state.groups, state.tunnelItems.map { it.config }),
                    expanded = true,
                )
            )
        onMoveToGroup(groupId, tunnels)
    }

    fun onSelectTunnels(tunnels: List<TunnelConfig>) = intent {
        reduce { state.copy(selectedTunnels = tunnels, isSelectionMode = tunnels.isNotEmpty()) }
    }

    fun onMoveToGroup(groupId: Long, tunnels: List<TunnelConfig>) = intent {
        if (tunnels.isEmpty()) return@intent
        val all = state.tunnelItems.map { it.config }
        var next = nextChildPosition(all, groupId)
        val ids = tunnels.map { it.id }.toSet()
        tunnelRepository.updateAll(
            all.map { tunnel ->
                if (tunnel.id in ids) tunnel.copy(groupId = groupId, position = next++) else tunnel
            }
        )
        reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
    }

    fun onUngroupTunnels(tunnels: List<TunnelConfig>) = intent {
        val grouped = tunnels.filter { it.groupId != null }
        if (grouped.isEmpty()) return@intent
        val all = state.tunnelItems.map { it.config }
        var next = nextRootPosition(state.groups, all)
        val ids = grouped.map { it.id }.toSet()
        tunnelRepository.updateAll(
            all.map { tunnel ->
                if (tunnel.id in ids) tunnel.copy(groupId = null, position = next++) else tunnel
            }
        )
        reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
    }
}

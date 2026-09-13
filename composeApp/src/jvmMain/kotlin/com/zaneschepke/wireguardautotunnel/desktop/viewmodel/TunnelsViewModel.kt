package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_cancelled
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.exported_to_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_tunnels_selected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_not_found
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown_error
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelUiItem
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
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
        val list = state.tunnelItems.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        reduce { state.copy(tunnelItems = list) }
    }

    fun onPersistReorder() = intent {
        val updatedTunnels =
            state.tunnelItems.mapIndexed { index, item -> item.config.copy(position = index) }
        tunnelRepository.updateAll(updatedTunnels)
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
                reduce { state.copy(isSelectionMode = false) }
            }
            is DeleteIntent.Tunnel -> {
                tunnelRepository.delete(intent.tunnel.id)
            }
        }
    }
}

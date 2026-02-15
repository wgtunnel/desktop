package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelService
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.DeleteIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.ExportIntent
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelsUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.FileUtils
import com.zaneschepke.wireguardautotunnel.desktop.util.asUserMessage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TunnelsViewModel(
    private val tunnelRepository: TunnelRepository,
    private val backendService: BackendService,
    private val tunnelService: TunnelService,
    private val tunnelImportService: TunnelImportService,
) : ContainerHost<TunnelsUiState, AppSideEffect>, ViewModel() {

    override val container =
        container<TunnelsUiState, AppSideEffect>(
            TunnelsUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent {
                tunnelRepository.flow.collect { tunnels ->
                    reduce { state.copy(tunnels = tunnels, isLoaded = true) }
                }
            }
            intent {
                backendService
                    .statusFlow()
                    .map { it.activeTunnels }
                    .distinctUntilChanged()
                    .collect { reduce { state.copy(tunnelStates = it) } }
            }
        }

    fun onItemsReordered(fromIndex: Int, toIndex: Int) = intent {
        val list = state.tunnels.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        reduce { state.copy(tunnels = list) }
    }

    fun onPersistReorder() = intent {
        val updatedTunnels =
            state.tunnels.mapIndexed { index, tunnel -> tunnel.copy(position = index) }
        tunnelRepository.updateAll(updatedTunnels)
    }

    fun onStartTunnel(id: Long) = intent {
        tunnelService.startTunnel(id).onFailure {
            val message = (it as? ClientException).asUserMessage()
            postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
        }
    }

    fun onStopTunnel(id: Long) = intent {
        tunnelService.stopTunnel(id).onFailure {
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
        tunnelImportService.import(configMap)
    }

    fun onConfImport(quickString: String, name: String?) = intent {
        tunnelImportService.import(quickString, name)
    }

    fun onExportIntent(intent: ExportIntent) = intent {
        val (file, bytes) =
            when (intent) {
                ExportIntent.Selected -> {
                    if (state.selectedTunnels.isEmpty()) {
                        postSideEffect(
                            AppSideEffect.Toast("No tunnels selected", ToastType.Warning)
                        )
                        return@intent
                    }
                    val configMap = state.selectedTunnels.associate { it.name to it.quickConfig }
                    val zipBytes = FileUtils.createZipArchive(configMap)
                    FileKit.openFileSaver("tunnels", extension = FileUtils.ZIP_FILE_EXTENSION) to
                        zipBytes
                }
                is ExportIntent.Tunnel -> {
                    FileKit.openFileSaver(
                        intent.tunnel.name,
                        extension = FileUtils.CONF_FILE_EXTENSION,
                    ) to intent.tunnel.quickConfig.toByteArray()
                }
            }

        try {
            if (file != null) {
                file.write(bytes)
                postSideEffect(AppSideEffect.Toast("Exported to ${file.name}", ToastType.Success))
            } else {
                postSideEffect(AppSideEffect.Toast("Export cancelled", ToastType.Info))
            }
        } catch (e: Exception) {
            postSideEffect(AppSideEffect.Toast("Export failed: ${e.message}", ToastType.Error))
        }
        reduce { state.copy(selectedTunnels = emptyList(), isSelectionMode = false) }
    }

    fun onSelectAll() = intent {
        reduce { state.copy(isSelectionMode = true, selectedTunnels = state.tunnels) }
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

package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelUiState
import com.zaneschepke.wireguardautotunnel.parser.Config
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TunnelViewModel(
    private val backendService: BackendService,
    private val tunnelRepository: TunnelRepository,
    val tunnelId: Long,
) : ContainerHost<TunnelUiState, AppSideEffect>, ViewModel() {

    override val container =
        container<TunnelUiState, AppSideEffect>(
            TunnelUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                tunnelRepository.flow
                    .map { it.firstOrNull { tun -> tun.id == tunnelId } }
                    .collect { tunnel ->
                        reduce {
                            state.copy(
                                isLoaded = true,
                                originalConfig = tunnel ?: state.originalConfig,
                                editedConfig = tunnel ?: state.editedConfig,
                            )
                        }
                    }
            }
            intent {
                backendService
                    .statusFlow()
                    .map { status ->
                        status.activeTunnels.firstOrNull { tunnel -> tunnel.id == tunnelId }
                    }
                    .collect {
                        reduce {
                            state.copy(
                                tunnelState = it?.state ?: state.tunnelState,
                                activeConfig = it?.activeConfig ?: state.activeConfig,
                            )
                        }
                    }
            }
        }

    fun onConfigUpdate(newText: String) = intent {
        val newEdited = state.editedConfig.copy(quickConfig = newText)
        reduce { state.copy(editedConfig = newEdited, isDirty = state.originalConfig != newEdited) }
    }

    fun onNameUpdated(name: String) = intent {
        val newEdited = state.editedConfig.copy(name = name)
        reduce { state.copy(editedConfig = newEdited, isDirty = state.originalConfig != newEdited) }
    }

    fun saveChanges() = intent {
        val sanitizedName = state.editedConfig.name.trim()

        val sanitizedQuick =
            state.editedConfig.quickConfig.lines().joinToString("\n") { it.trimEnd() }.trim()

        val sanitizedConfig =
            state.editedConfig.copy(name = sanitizedName, quickConfig = sanitizedQuick)

        runCatching { Config.parseQuickString(sanitizedConfig.quickConfig) }
            .onSuccess {
                tunnelRepository.save(sanitizedConfig)

                reduce {
                    state.copy(
                        isDirty = false,
                        originalConfig = sanitizedConfig,
                        editedConfig = sanitizedConfig,
                    )
                }
                postSideEffect(AppSideEffect.Toast("Config saved successfully!", ToastType.Success))
            }
            .onFailure {
                postSideEffect(
                    AppSideEffect.Toast("Invalid Config: ${it.message}", ToastType.Error)
                )
            }
    }
}

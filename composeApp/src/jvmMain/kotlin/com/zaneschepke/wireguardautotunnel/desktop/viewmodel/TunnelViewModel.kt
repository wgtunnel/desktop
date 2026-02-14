package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendCommandService
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelUiState
import com.zaneschepke.wireguardautotunnel.parser.Config
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TunnelViewModel(
    private val backendCommandService: BackendCommandService,
    private val tunnelRepository: TunnelRepository,
    val tunnelId: Long,
) : ContainerHost<TunnelUiState, AppSideEffect>, ViewModel() {

    override val container =
        container<TunnelUiState, AppSideEffect>(
            TunnelUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            combine(
                    tunnelRepository.flow.map { it.firstOrNull { tun -> tun.id == tunnelId } },
                    backendCommandService.statusFlow().map { status ->
                        status.activeTunnels.firstOrNull { tunnel -> tunnel.id == tunnelId }
                    },
                ) { tunnel, activeTunnel ->
                    tunnel to activeTunnel
                }
                .collect { (tunnel, status) ->
                    reduce {
                        state.copy(
                            isLoaded = true,
                            originalConfig = tunnel ?: state.originalConfig,
                            editedConfig = tunnel ?: state.editedConfig,
                            tunnelState = status?.state,
                            activeConfig = status?.activeConfig ?: state.activeConfig,
                        )
                    }
                }
        }

    fun onConfigUpdate(newText: String) = intent {
        reduce {
            state.copy(
                editedConfig = state.editedConfig.copy(quickConfig = newText),
                isDirty = state.originalConfig != state.editedConfig,
            )
        }
    }

    fun onNameUpdated(name: String) = intent {
        reduce {
            val updatedConfig = state.editedConfig.copy(name = name)
            state.copy(
                editedConfig = updatedConfig,
                isDirty = state.originalConfig != state.editedConfig,
            )
        }
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

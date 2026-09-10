package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.config_changes_saved
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_name_empty
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.TunnelUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.toConfigErrorMessage
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class TunnelViewModel(
    private val backendService: BackendService,
    private val tunnelRepository: TunnelRepository,
    private val tunnelCoordinator: TunnelCoordinator,
    val tunnelId: Long,
) : OrbitContainerHost<TunnelUiState, TunnelUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<TunnelUiState, AppSideEffect>(
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
                                currentConfig = tunnel ?: state.currentConfig,
                                editedConfig =
                                    if (state.isDirty) state.editedConfig
                                    else tunnel ?: state.editedConfig,
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
                                activeConfig = it?.activeConfig ?: state.activeConfig,
                                lastStatsAtMs =
                                    it?.let { status -> state.lastStatsAtMs }
                                        ?: state.lastStatsAtMs,
                            )
                        }
                    }
            }
        }

    fun onConfigUpdate(newText: String) = intent {
        val newEdited = state.editedConfig.copy(quickConfig = newText)
        reduce { state.copy(editedConfig = newEdited, isDirty = state.currentConfig != newEdited) }
    }

    fun onNameUpdated(name: String) = intent {
        val newEdited = state.editedConfig.copy(name = name)
        reduce { state.copy(editedConfig = newEdited, isDirty = state.currentConfig != newEdited) }
    }

    fun togglePrimaryTunnel() = intent {
        val tunnel = state.currentConfig
        if (tunnel.id == 0L) return@intent
        val update = if (tunnel.isPrimaryTunnel) null else tunnel
        tunnelRepository.updatePrimaryTunnel(update)
    }

    fun onDdnsTunnel(enabled: Boolean) = intent {
        tunnelRepository.setDdnsTunnel(tunnelId, enabled)
    }

    fun onIpv6Preferred(enabled: Boolean) = intent {
        val tunnel = state.currentConfig
        val updated =
            if (!enabled) {
                tunnel.copy(preferIpv6 = false, ipv6RestoreEnabled = false)
            } else {
                tunnel.copy(preferIpv6 = true)
            }
        tunnelRepository.save(updated)
    }

    fun onIpv6Restore(enabled: Boolean) = intent {
        tunnelRepository.save(state.currentConfig.copy(ipv6RestoreEnabled = enabled))
    }

    fun saveChanges(restart: Boolean = false) = intent {
        val sanitizedName = state.editedConfig.name.trim()
        if (sanitizedName.isEmpty()) {
            postSideEffect(
                AppSideEffect.Toast(getString(Res.string.tunnel_name_empty), ToastType.Error)
            )
            return@intent
        }
        val sanitizedQuick =
            state.editedConfig.quickConfig.lines().joinToString("\n") { it.trimEnd() }.trim()
        val sanitizedConfig =
            state.editedConfig.copy(name = sanitizedName, quickConfig = sanitizedQuick)

        runCatching {
            val parsed = Config.parseQuickString(sanitizedConfig.quickConfig)
            parsed.validate()
            parsed
        }
            .onSuccess { parsed ->
                val toSave = sanitizedConfig.copy(quickConfig = parsed.asQuickString())
                val wasRunning = state.isRunning
                tunnelRepository.save(toSave)
                reduce {
                    state.copy(
                        isDirty = false,
                        currentConfig = toSave,
                        editedConfig = toSave,
                    )
                }
                if (restart && wasRunning) {
                    tunnelCoordinator.stopTunnel(tunnelId)
                    tunnelCoordinator.startTunnel(toSave)
                }
                postSideEffect(
                    AppSideEffect.Toast(
                        getString(Res.string.config_changes_saved),
                        ToastType.Success,
                    )
                )
            }
            .onFailure {
                postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
            }
    }
}

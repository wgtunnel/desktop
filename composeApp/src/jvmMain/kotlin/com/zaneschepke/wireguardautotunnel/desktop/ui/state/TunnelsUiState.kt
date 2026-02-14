package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus

data class TunnelsUiState(
    val tunnels: List<TunnelConfig> = emptyList(),
    val tunnelStates: List<TunnelStatus> = emptyList(),
    val selectedTunnels: List<TunnelConfig> = emptyList(),
    val isSelectionMode: Boolean = false,
    val isLoaded: Boolean = false,
)

package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.NetworkStatusDto

data class AutoTunnelUiState(
    val isLoaded: Boolean = false,
    val autoTunnelSettings: AutoTunnelSettings = AutoTunnelSettings(),
    val tunnels: List<TunnelConfig> = emptyList(),
    val network: NetworkStatusDto = NetworkStatusDto(),
    val autoTunnelActive: Boolean = false,
)

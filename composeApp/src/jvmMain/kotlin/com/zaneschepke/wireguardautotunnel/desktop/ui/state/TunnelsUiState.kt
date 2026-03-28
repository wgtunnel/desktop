package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import androidx.compose.ui.graphics.Color
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asColor
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components.asTooltipMessage

data class TunnelsUiState(
    val tunnelItems: List<TunnelUiItem> = emptyList(),
    val selectedTunnels: List<TunnelConfig> = emptyList(),
    val isSelectionMode: Boolean = false,
    val isLoaded: Boolean = false,
)

data class TunnelUiItem(val config: TunnelConfig, val status: TunnelStatus? = null) {
    val isRunning: Boolean
        get() = status?.state != TunnelState.DOWN && status?.state != TunnelState.STOPPING && status != null

    val stateColor: Color
        get() = status?.state?.asColor() ?: TunnelState.DOWN.asColor()

    val tooltipMessage: String
        get() = status?.state?.asTooltipMessage() ?: ""
}

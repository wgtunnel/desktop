package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.ui.graphics.Color
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.HealthyGreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WarningAmber

fun TunnelState.asColor(): Color {
    return when (this) {
        TunnelState.DOWN -> Color.Gray
        TunnelState.HEALTHY -> HealthyGreen
        TunnelState.HANDSHAKE_FAILURE -> ErrorRed
        TunnelState.RESOLVING_DNS,
        TunnelState.STARTING,
        TunnelState.STOPPING -> WarningAmber
    }
}

fun TunnelState.asTooltipMessage(): String {
    return when (this) {
        TunnelState.DOWN,
        TunnelState.STARTING -> "Starting"
        TunnelState.STOPPING -> "Stopping"
        TunnelState.HEALTHY -> "Healthy"
        TunnelState.HANDSHAKE_FAILURE -> "Handshake failure"
        TunnelState.RESOLVING_DNS -> "Resolving DNS"
    }
}

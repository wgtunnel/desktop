package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.ui.graphics.Color
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.Straw

fun TunnelState.asColor(): Color {
    return when (this) {
        TunnelState.DOWN,
        TunnelState.STARTING,
        TunnelState.UNKNOWN -> Color.Gray
        TunnelState.HEALTHY -> SilverTree
        TunnelState.HANDSHAKE_FAILURE -> AlertRed
        TunnelState.RESOLVING_DNS -> Straw
    }
}

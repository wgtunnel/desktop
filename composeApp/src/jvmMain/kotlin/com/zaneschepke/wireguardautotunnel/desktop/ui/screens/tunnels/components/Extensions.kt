package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.lockdown_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.proxy_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown_mode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.vpn_mode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.HealthyGreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WarningAmber
import org.jetbrains.compose.resources.stringResource

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

/**
 * Status-line label matching Android's DisplayTunnelState wording. Null for states that shouldn't
 * be shown at all (down, or stopping -- which clears rather than lingers).
 */
fun TunnelState.asStatusLabel(): String? {
    return when (this) {
        TunnelState.DOWN,
        TunnelState.STOPPING -> null
        TunnelState.STARTING -> "Establishing connection"
        TunnelState.RESOLVING_DNS -> "Resolving DNS"
        TunnelState.HEALTHY -> "Connected"
        TunnelState.HANDSHAKE_FAILURE -> "Handshake failure"
    }
}

@Composable
fun BackendMode.asTooltipMessage(): String {
    return when (this) {
        BackendMode.VPN -> stringResource(Res.string.vpn_mode)
        BackendMode.PROXY -> stringResource(Res.string.proxy_mode)
        BackendMode.LOCK_DOWN -> stringResource(Res.string.lockdown_mode)
        BackendMode.UNKNOWN -> stringResource(Res.string.unknown_mode)
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_connected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_establishing_connection
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_handshake_failure
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_healthy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_resolving_dns
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_starting
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_state_stopping
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

@Composable
fun TunnelState.asTooltipMessage(): String {
    return when (this) {
        TunnelState.DOWN,
        TunnelState.STARTING -> stringResource(Res.string.tunnel_state_starting)
        TunnelState.STOPPING -> stringResource(Res.string.tunnel_state_stopping)
        TunnelState.HEALTHY -> stringResource(Res.string.tunnel_state_healthy)
        TunnelState.HANDSHAKE_FAILURE -> stringResource(Res.string.tunnel_state_handshake_failure)
        TunnelState.RESOLVING_DNS -> stringResource(Res.string.tunnel_state_resolving_dns)
    }
}

@Composable
fun TunnelState.asStatusLabel(): String? {
    return when (this) {
        TunnelState.DOWN,
        TunnelState.STOPPING -> null
        TunnelState.STARTING -> stringResource(Res.string.tunnel_state_establishing_connection)
        TunnelState.RESOLVING_DNS -> stringResource(Res.string.tunnel_state_resolving_dns)
        TunnelState.HEALTHY -> stringResource(Res.string.tunnel_state_connected)
        TunnelState.HANDSHAKE_FAILURE -> stringResource(Res.string.tunnel_state_handshake_failure)
    }
}

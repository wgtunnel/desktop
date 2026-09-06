package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.wgtunnel.backend.state.ActiveTunnel
import kotlin.time.Duration.Companion.milliseconds
import nl.jacobras.humanreadable.HumanReadable

@Composable
fun TunnelOverviewSection(activeTunnel: ActiveTunnel, now: Long) {
    val style = MaterialTheme.typography.bodySmall
    val color = MaterialTheme.colorScheme.outline
    val status =
        activeTunnel.transportState::class.simpleName?.lowercase()?.replace('_', ' ') ?: "unknown"
    val uptime =
        activeTunnel.uptime
            ?.takeIf { it > 0L }
            ?.let { startedAt ->
                HumanReadable.duration((now - startedAt).coerceAtLeast(0L).milliseconds)
            }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "status: $status", style = style, color = color)
        uptime?.let { Text(text = "uptime: $it", style = style, color = color) }
    }
}

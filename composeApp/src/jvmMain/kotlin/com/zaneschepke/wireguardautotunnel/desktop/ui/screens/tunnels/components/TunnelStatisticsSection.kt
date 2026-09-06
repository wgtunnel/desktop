package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
fun TunnelStatisticsSection(
    status: TunnelStatus?,
    modifier: Modifier = Modifier,
) {
    val now by
        produceState(System.currentTimeMillis()) {
            while (true) {
                delay(1.seconds)
                value = System.currentTimeMillis()
            }
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val style = MaterialTheme.typography.bodySmall
        val color = MaterialTheme.colorScheme.outline

        if (status == null || status.state == TunnelState.DOWN) return@Column

        StatText(
            text = "status: ${status.state.name.lowercase().replace('_', ' ')}",
            style = style,
            color = color,
        )
        StatText(
            text = "mode: ${status.mode.name.lowercase().replace('_', ' ')}",
            style = style,
            color = color,
        )
        if (status.recoveryAttempts > 0) {
            StatText(
                text = "recovery attempts: ${status.recoveryAttempts}",
                style = style,
                color = color,
            )
        }

        val config = status.activeConfig
        if (config == null) {
            Text("Waiting for statistics…", style = style, color = color)
        } else {
            config.peers.forEach { peer -> PeerStatisticsSection(peer = peer, now = now) }
        }
    }
}

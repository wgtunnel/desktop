package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.wgtunnel.parser.ActivePeer
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.never
import com.zaneschepke.wireguardautotunnel.desktop.util.abbreviateKey
import com.zaneschepke.wireguardautotunnel.desktop.util.formatFileSize
import com.zaneschepke.wireguardautotunnel.desktop.util.toAgoDisplay
import org.jetbrains.compose.resources.stringResource

@Composable
fun PeerStatisticsSection(peer: ActivePeer, now: Long) {
    val style = MaterialTheme.typography.bodySmall
    val color = MaterialTheme.colorScheme.outline

    val rx = (peer.rxBytes ?: 0L).formatFileSize()
    val tx = (peer.txBytes ?: 0L).formatFileSize()
    val handshake =
        peer.lastHandshakeSeconds.toAgoDisplay(now) ?: stringResource(Res.string.never).lowercase()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatText(text = "peer: ${peer.publicKey.abbreviateKey()}", style = style, color = color)
        TransferStatsRow(rx = rx, tx = tx, style = style, color = color)
        StatText(text = "last handshake: $handshake", style = style, color = color)
        peer.endpoint?.let { StatText(text = "endpoint: $it", style = style, color = color) }
    }
}

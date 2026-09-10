package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.recovery_attempts_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.status_lowercase_template
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

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

        val statusLabel = status?.state?.asStatusLabel() ?: return@Column

        StatText(
            text = stringResource(Res.string.status_lowercase_template, statusLabel),
            style = style,
            color = color,
        )
        if (status.recoveryAttempts > 0) {
            StatText(
                text =
                    stringResource(
                        Res.string.recovery_attempts_template,
                        status.recoveryAttempts,
                    ),
                style = style,
                color = color,
            )
        }

        status.activeConfig?.peers?.forEach { peer ->
            PeerStatisticsSection(peer = peer, now = now)
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.wgtunnel.backend.state.ActiveTunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.status_lowercase_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.uptime_lowercase_template
import kotlin.time.Duration.Companion.milliseconds
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun TunnelOverviewSection(activeTunnel: ActiveTunnel, now: Long) {
    val style = MaterialTheme.typography.bodySmall
    val color = MaterialTheme.colorScheme.outline
    val status =
        activeTunnel.transportState::class.simpleName?.lowercase()?.replace('_', ' ')
            ?: stringResource(Res.string.unknown)
    val uptime =
        activeTunnel.uptime
            ?.takeIf { it > 0L }
            ?.let { startedAt ->
                HumanReadable.duration((now - startedAt).coerceAtLeast(0L).milliseconds)
            }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.status_lowercase_template, status),
            style = style,
            color = color,
        )
        uptime?.let {
            Text(
                text = stringResource(Res.string.uptime_lowercase_template, it),
                style = style,
                color = color,
            )
        }
    }
}

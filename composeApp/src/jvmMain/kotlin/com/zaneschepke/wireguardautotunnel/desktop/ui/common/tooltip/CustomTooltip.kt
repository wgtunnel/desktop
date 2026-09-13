package com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.requires_daemon_running
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTooltip(
    modifier: Modifier = Modifier,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.End,
    text: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = {
            PlainTooltip(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                Text(text, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        },
        state = rememberTooltipState(),
    ) {
        content()
    }
}

@Composable
fun DisabledReasonTooltip(
    enabled: Boolean,
    reason: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (enabled) {
        content()
    } else {
        CustomTooltip(modifier = modifier, text = reason, content = content)
    }
}

// Standard wrapper for any toggle/button that requires the daemon to be running.
@Composable
fun RequiresDaemonTooltip(
    daemonConnected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DisabledReasonTooltip(
        enabled = daemonConnected,
        reason = stringResource(Res.string.requires_daemon_running),
        modifier = modifier,
        content = content,
    )
}

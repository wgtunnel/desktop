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

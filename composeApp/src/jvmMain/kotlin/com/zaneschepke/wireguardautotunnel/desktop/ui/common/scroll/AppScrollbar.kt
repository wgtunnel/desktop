package com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ScrollbarThickness = 8.dp

@Composable
fun appScrollbarStyle(thickness: Dp = ScrollbarThickness): ScrollbarStyle =
    defaultScrollbarStyle()
        .copy(
            thickness = thickness,
            unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
            hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )

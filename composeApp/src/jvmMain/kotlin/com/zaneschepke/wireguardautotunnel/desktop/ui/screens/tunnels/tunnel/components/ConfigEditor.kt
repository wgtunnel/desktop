package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfigEditor(rawConfig: String, isEditable: Boolean, onConfigChange: (String) -> Unit) {
    val scrollState = rememberScrollState()

    var quick by remember { mutableStateOf(rawConfig) }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    if (isEditable) MaterialTheme.colorScheme.surfaceContainerLowest
                    else MaterialTheme.colorScheme.surface
                )
    ) {
        BasicTextField(
            value = quick,
            onValueChange = {
                if (isEditable) {
                    quick = it
                    onConfigChange(it)
                }
            },
            readOnly = !isEditable,
            modifier =
                Modifier.fillMaxSize()
                    .padding(end = 12.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
            visualTransformation = ConfigVisualTransformation(),
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush =
                if (isEditable) SolidColor(MaterialTheme.colorScheme.primary)
                else SolidColor(Color.Transparent),
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState),
            style =
                defaultScrollbarStyle()
                    .copy(
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    ),
        )
    }
}

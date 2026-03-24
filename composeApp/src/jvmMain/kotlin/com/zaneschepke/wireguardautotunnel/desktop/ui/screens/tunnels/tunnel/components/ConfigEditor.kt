package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun ConfigEditor(
    rawConfig: String,
    isEditable: Boolean,
    onConfigChange: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(rawConfig)) }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // One time sync
    LaunchedEffect(rawConfig) {
        if (textFieldValue.text != rawConfig) {
            textFieldValue = TextFieldValue(rawConfig)
        }
    }

    // Debounce changes
    LaunchedEffect(textFieldValue) {
        snapshotFlow { textFieldValue.text }
            .debounce(100)
            .onEach { onConfigChange(it) }
            .launchIn(this)
    }

    val scrollbarStyle = defaultScrollbarStyle().copy(
        thickness = 10.dp,
        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isEditable) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.surface
            )
            .clipToBounds()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (isEditable) {
                    textFieldValue = newValue
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(16.dp)
                .padding(end = 26.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            ),
            readOnly = !isEditable,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = remember { ConfigVisualTransformation() },
        )

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = scrollbarStyle,
        )

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(end = 26.dp),
            style = scrollbarStyle,
        )
    }
}
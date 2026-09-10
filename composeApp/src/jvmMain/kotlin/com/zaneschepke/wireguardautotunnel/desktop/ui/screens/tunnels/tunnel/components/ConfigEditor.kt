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
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollbarThickness
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.appScrollbarStyle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val ContentPadding = 16.dp

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun ConfigEditor(
    rawConfig: String,
    isEditable: Boolean,
    onConfigChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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

    // Debounce changes; keyed on Unit so this observes for the composition's lifetime instead of
    // restarting the flow collection on every keystroke.
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldValue.text }
            .debounce(100.milliseconds)
            .onEach { onConfigChange(it) }
            .launchIn(this)
    }

    val scrollbarStyle = appScrollbarStyle()

    Box(
        modifier =
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clipToBounds()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (isEditable) {
                    textFieldValue = newValue
                }
            },
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(ContentPadding)
                    .padding(end = ScrollbarThickness, bottom = ScrollbarThickness),
            textStyle =
                TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                ),
            readOnly = !isEditable,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = remember { ConfigVisualTransformation() },
        )

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(bottom = ScrollbarThickness),
            style = scrollbarStyle,
        )

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(end = ScrollbarThickness),
            style = scrollbarStyle,
        )
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberSyncedTextFieldState(
    value: String,
    onValueChange: (String) -> Unit,
): Pair<TextFieldValue, (TextFieldValue) -> Unit> {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var lastSentValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastSentValue) {
            textFieldValue = TextFieldValue(value)
            lastSentValue = value
        }
    }

    val onTextFieldValueChange: (TextFieldValue) -> Unit = { newValue ->
        textFieldValue = newValue
        if (newValue.text != lastSentValue) {
            lastSentValue = newValue.text
            onValueChange(newValue.text)
        }
    }

    return textFieldValue to onTextFieldValueChange
}

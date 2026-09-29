package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.group_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.add_group
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.okay
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox.ConfigurationTextBox
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val attest = {
        if (name.isBlank()) {
            isError = true
        } else {
            onConfirm(name.trim())
        }
    }

    InfoDialog(
        onDismiss = onDismiss,
        title = title,
        body = {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier =
                    Modifier.fillMaxWidth().onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (it.key) {
                            Key.Escape -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                ConfigurationTextBox(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = stringResource(Res.string.group_name),
                    hint = stringResource(Res.string.add_group),
                    isError = isError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { attest() }),
                    focusRequester = focusRequester,
                )
            }
        },
        confirmText = stringResource(Res.string.okay),
        onAttest = attest,
    )
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.lockdown

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.*
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SettingsUiState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DirectWhitelistEditor(state: SettingsUiState, onSave: (String) -> Unit) {
    var draft by remember { mutableStateOf(TextFieldValue(state.directWhitelist)) }
    var clipboardError by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.directWhitelist, state.directWhitelistLoaded) {
        if (state.directWhitelistLoaded && draft.text != state.directWhitelist) {
            draft = TextFieldValue(state.directWhitelist)
        }
    }
    val enabled = state.daemonConnected && state.directWhitelistLoaded && !state.directWhitelistSaving
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.direct_whitelist_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.direct_whitelist_description), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            minLines = 4,
            maxLines = 10,
            placeholder = { Text("192.168.1.10\n203.0.113.0/24\nexample.com") },
        )
        state.directWhitelistError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (clipboardError) Text(stringResource(Res.string.direct_whitelist_clipboard_error), color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = enabled, onClick = {
                scope.launch {
                    runCatching {
                        val transferable = clipboard.getClipEntry()?.nativeClipEntry as? Transferable
                        val text = transferable?.getTransferData(DataFlavor.stringFlavor) as? String
                        if (text != null) {
                            val start = draft.selection.min
                            val end = draft.selection.max
                            draft = TextFieldValue(draft.text.replaceRange(start, end, text), TextRange(start + text.length))
                        }
                    }.onSuccess { clipboardError = false }.onFailure { clipboardError = true }
                }
            }) { Text(stringResource(Res.string.direct_whitelist_paste)) }
            Button(enabled = enabled && draft.text != state.directWhitelist, onClick = { onSave(draft.text) }) {
                Text(stringResource(Res.string.direct_whitelist_apply))
            }
        }
    }
}

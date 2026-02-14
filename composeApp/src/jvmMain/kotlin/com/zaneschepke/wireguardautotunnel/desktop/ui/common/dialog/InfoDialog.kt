package com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun InfoDialog(
    onAttest: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    body: @Composable (() -> Unit),
    confirmText: String,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = { onDismiss() },
                confirmButton = {
                    TextButton(onClick = { onAttest() }) { Text(text = confirmText) }
                },
                dismissButton = {
                    TextButton(onClick = { onDismiss() }) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(text = title) },
                text = { body() },
                properties = DialogProperties(usePlatformDefaultWidth = true),
            )
        }
    }
}

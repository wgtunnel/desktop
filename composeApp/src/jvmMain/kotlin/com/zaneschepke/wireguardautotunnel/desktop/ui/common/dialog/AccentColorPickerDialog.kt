package com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.cancel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.choose_accent_color
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccentColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(initialColor) }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = { onConfirm(selectedColor) }) {
                        Text(text = stringResource(Res.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(text = stringResource(Res.string.choose_accent_color)) },
                text = {
                    Column {
                        HsvColorPicker(
                            modifier = Modifier.fillMaxWidth().height(300.dp).padding(10.dp),
                            controller = controller,
                            initialColor = initialColor,
                            onColorChanged = { envelope: ColorEnvelope ->
                                selectedColor = envelope.color
                            },
                        )
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = true),
            )
        }
    }
}

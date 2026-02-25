package com.zaneschepke.wireguardautotunnel.desktop.ui.common.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select_window_2
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wgtunnel
import java.awt.Frame
import java.awt.event.WindowStateListener
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun WindowScope.TitleBar(onClose: () -> Unit) {
    val frame = window as? Frame

    // sync state with frame
    var isMaximized by remember { mutableStateOf(frame?.extendedState == Frame.MAXIMIZED_BOTH) }

    DisposableEffect(frame) {
        val listener = WindowStateListener { e ->
            isMaximized = (e.newState and Frame.MAXIMIZED_BOTH) != 0
        }
        frame?.addWindowStateListener(listener)
        onDispose { frame?.removeWindowStateListener(listener) }
    }

    WindowDraggableArea {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.wgtunnel),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WindowControlButton(Icons.Default.Remove, "Minimize") {
                    frame?.state = Frame.ICONIFIED
                }

                WindowControlButton(
                    if (isMaximized) vectorResource(Res.drawable.select_window_2)
                    else Icons.Default.CropSquare,
                    if (isMaximized) "Restore" else "Maximize",
                ) {
                    frame?.extendedState = if (isMaximized) Frame.NORMAL else Frame.MAXIMIZED_BOTH
                }

                WindowControlButton(Icons.Default.Close, "Close") { onClose() }
            }
        }
    }
}

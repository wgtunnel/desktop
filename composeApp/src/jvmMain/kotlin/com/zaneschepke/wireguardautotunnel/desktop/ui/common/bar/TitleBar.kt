package com.zaneschepke.wireguardautotunnel.desktop.ui.common.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.select_window_2
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.wgtunnel
import java.awt.Frame
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun WindowScope.TitleBar(onClose: () -> Unit) {
    val frame = window as? Frame
    var isMaximized by remember {
        mutableStateOf((frame?.extendedState ?: Frame.NORMAL) == Frame.MAXIMIZED_BOTH)
    }

    val buttonSize = 18.dp

    val colors =
        IconButtonDefaults.iconButtonColors()
            .copy(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            )

    WindowDraggableArea {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.wgtunnel),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { frame?.state = Frame.ICONIFIED },
                    colors = colors,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Minimize",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = {
                        val f = frame ?: return@IconButton
                        f.extendedState = if (isMaximized) Frame.NORMAL else Frame.MAXIMIZED_BOTH
                        isMaximized = !isMaximized
                    },
                    colors = colors,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector =
                            if (isMaximized) vectorResource(Res.drawable.select_window_2)
                            else Icons.Default.CropSquare,
                        contentDescription = if (isMaximized) "Restore" else "Maximize",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = onClose,
                    colors = colors,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

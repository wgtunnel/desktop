package com.zaneschepke.wireguardautotunnel.desktop.ui.common.button

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SwitchWithDivider(
    checked: Boolean,
    onClick: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onDisabledClick: () -> Unit = {},
) {
    PreferenceTrailing {
        Box(
            modifier =
                Modifier.pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures {}
                    } else {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            down.consume()
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            up?.consume()
                            if (up != null) onDisabledClick()
                        }
                    }
                }
        ) {
            ThemedSwitch(
                checked = checked,
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
            )
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.icerock.moko.resources.compose.stringResource
import com.zaneschepke.wireguardautotunnel.SharedRes

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(SharedRes.strings.app_name)
    ) {
        App()
    }
}
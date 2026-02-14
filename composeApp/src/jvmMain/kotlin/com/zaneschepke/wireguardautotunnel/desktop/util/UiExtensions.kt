package com.zaneschepke.wireguardautotunnel.desktop.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
fun String.toClipEntry(): ClipEntry {
    return ClipEntry(StringSelection(this))
}

// TODO improve and localize
fun ClientException?.asUserMessage(): String {
    return when (this) {
        is ClientException.BadRequestException -> "Invalid request, please try again."
        is ClientException.ConflictException -> "This action has already been performed."
        is ClientException.DaemonCommsException ->
            "Daemon communication error, please check the daemon status."
        is ClientException.InternalServerError -> "An internal error occurred, please try again."
        is ClientException.UnknownError,
        null -> "An unknown error occurred, please try again."
    }
}

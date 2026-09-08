package com.zaneschepke.wireguardautotunnel.desktop.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import com.wgtunnel.parser.ConfigParseException
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.auth_error
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.config_error
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.config_error_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.error_bad_request
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.error_conflict
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.error_daemon_comms
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.error_internal_server
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.invalid_config_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown_error
import java.awt.datatransfer.StringSelection
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalComposeUiApi::class)
fun String.toClipEntry(): ClipEntry {
    return ClipEntry(StringSelection(this))
}

suspend fun ClientException?.asUserMessage(): String {
    return when (this) {
        is ClientException.BadRequestException -> getString(Res.string.error_bad_request)
        is ClientException.ConflictException -> getString(Res.string.error_conflict)
        is ClientException.DaemonCommsException -> getString(Res.string.error_daemon_comms)
        is ClientException.InternalServerError -> getString(Res.string.error_internal_server)
        is ClientException.UnauthorizedException -> getString(Res.string.auth_error)
        is ClientException.UnknownError,
        null -> getString(Res.string.unknown_error)
    }
}

suspend fun Throwable.toConfigErrorMessage(): String {
    return when (this) {
        is ConfigParseException ->
            getString(Res.string.config_error_template, errorType.name, field)
        else ->
            message?.takeIf { it.isNotBlank() }?.let { getString(Res.string.invalid_config_template, it) }
                ?: getString(Res.string.config_error)
    }
}

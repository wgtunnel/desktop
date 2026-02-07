package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.KillSwitchRequest
import com.zaneschepke.wireguardautotunnel.daemon.dto.toDto
import com.zaneschepke.wireguardautotunnel.daemon.dto.toInternal
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import com.zaneschepke.wireguardautotunnel.tunnel.util.BackendException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

fun Route.backendRoutes(backend: Backend) {

    post(Routes.BACKEND_MODE) {
        val mode = call.receive<BackendMode>()

        Logger.i { "Setting backend mode to $mode" }
        backend.setMode(mode.toInternal())
        call.respond(HttpStatusCode.OK, "Backend mode set to $mode")
    }

    post(Routes.BACKEND_KILL_SWITCH) {
        val request = call.receive<KillSwitchRequest>()

        Logger.i { "Setting kill switch to enabled: ${request.enable} and bypassLan: ${request.bypassLan}" }
        backend.setKillSwitch(request.enable).onSuccess {
            call.respond(HttpStatusCode.OK, "Kill switch set to ${request.enable} successfully")
        }.onFailure {
            if(it is BackendException.KillSwitchAlreadyActivate) call.respond(HttpStatusCode.BadRequest, "Kill switch is already in this state.")
            else call.respond(HttpStatusCode.InternalServerError, "Failed to toggle kill switch")
        }
    }

    get(Routes.BACKEND_STATUS) {
        val status = backend.status.first()
        call.respond(HttpStatusCode.OK, status.toDto())
    }

    webSocket(Routes.BACKEND_STATUS_WS) {
        Logger.i { "Client connected to backend status stream" }
        try {
            backend.status
                .map { it.toDto() }
                .collect { dto ->
                    Logger.d { "Daemon: Sending status update to WS: $dto" }
                    sendSerialized(dto)
                }
        } catch (e: Exception) {
            Logger.e(e) { "Error streaming status" }
        }
    }
}
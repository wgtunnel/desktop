package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
import com.zaneschepke.wireguardautotunnel.daemon.dto.toDto
import com.zaneschepke.wireguardautotunnel.daemon.dto.toInternal
import com.zaneschepke.wireguardautotunnel.parser.ActiveConfig
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import com.zaneschepke.wireguardautotunnel.tunnel.Tunnel
import com.zaneschepke.wireguardautotunnel.tunnel.util.BackendException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
fun Route.backendRoutes(backend: Backend) {

    put(Routes.BACKEND_MODE) {
        val mode = call.receive<BackendMode>()

        Logger.i { "Setting backend mode to $mode" }
        backend.setMode(mode.toInternal())
        call.respond(HttpStatusCode.OK, "Backend mode set to $mode")
    }

    put(Routes.BACKEND_KILL_SWITCH_BYPASS) {
        val request = call.receive<FlagRequest>()
        Logger.i { "Setting backend bypass lan to $request" }
        backend
            .setKillSwitchLanBypass(request.value)
            .onSuccess {
                call.respond(
                    HttpStatusCode.OK,
                    "Bypass LAN for kill switch set to ${request.value} successfully",
                )
            }
            .onFailure { call.respond(HttpStatusCode.BadRequest, it.message ?: "Unknown error") }
    }

    put(Routes.BACKEND_KILL_SWITCH) {
        val request = call.receive<FlagRequest>()

        Logger.i { "Setting kill switch to enabled: ${request.value}" }
        backend
            .setKillSwitch(request.value)
            .onSuccess {
                call.respond(HttpStatusCode.OK, "Kill switch set to ${request.value} successfully")
            }
            .onFailure {
                if (it is BackendException.StateConflict)
                    call.respond(HttpStatusCode.BadRequest, it.message)
                else
                    call.respond(HttpStatusCode.InternalServerError, "Failed to toggle kill switch")
            }
    }

    get(Routes.BACKEND_ACTIVE_CONFIG) {
        val id =
            call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")

        backend
            .getActiveConfig(id)
            .onSuccess { configStr ->
                if (configStr == null) {
                    call.respond(HttpStatusCode.NotFound, "No config available for tunnel $id")
                } else {
                    call.respondText(configStr, ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
            .onFailure { e ->
                Logger.e(e) { "Failed to get active config for tunnel $id" }
                if (e is BackendException.StateConflict) {
                    call.respond(HttpStatusCode.Conflict, e.message)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve config")
                }
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
                .distinctUntilChanged()
                .flatMapLatest { status ->
                    flow {
                        while (true) {
                            val tunnelStatuses =
                                status.activeTunnels.map { (key, state) ->
                                    val activeConfig =
                                        if (state is Tunnel.State.Up) {
                                            backend.getActiveConfig(key.id).getOrNull()?.let { str
                                                ->
                                                try {
                                                    ActiveConfig.parseFromIpc(str)
                                                } catch (e: Exception) {
                                                    Logger.e(e) {
                                                        "Failed to parse active config for tunnel ${key.id}"
                                                    }
                                                    null
                                                }
                                            }
                                        } else null
                                    TunnelStatus(key.id, key.name, state.toDto(), activeConfig)
                                }
                            val dto =
                                BackendStatus(
                                    status.killSwitchEnabled,
                                    status.mode.toDto(),
                                    tunnelStatuses,
                                )
                            emit(dto)
                            delay(3000)
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { dto ->
                    Logger.d { "Daemon: Sending status update to WS: $dto" }
                    sendSerialized(dto)
                }
        } catch (e: Exception) {
            Logger.e(e) { "Error streaming status" }
        }
    }
}

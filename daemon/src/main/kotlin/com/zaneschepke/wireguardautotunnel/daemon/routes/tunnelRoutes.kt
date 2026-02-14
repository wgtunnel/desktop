package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import com.zaneschepke.wireguardautotunnel.tunnel.util.BackendException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tunnelRoutes(backend: Backend) {

    post(Routes.Tunnels.START_TEMPLATE) {
        val id =
            call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")
        val request = call.receive<StartTunnelRequest>()

        Logger.i { "Starting tunnel (${request.name})" }

        val tunnel = RunningTunnel(id, request.name)

        backend
            .start(tunnel, request.quickConfig)
            .onSuccess { call.respond(HttpStatusCode.OK, "Tunnel ${request.name} started") }
            .onFailure {
                if (it is BackendException.StateConflict)
                    call.respond(HttpStatusCode.Conflict, it.message)
                else
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        it.message
                            ?: "Failed to start tunnel ${request.name} due to internal error.",
                    )
            }
    }

    post(Routes.Tunnels.STOP_TEMPLATE) {
        val id =
            call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")

        backend
            .stop(id.toLong())
            .onSuccess { call.respond(HttpStatusCode.OK, "Tunnel $id stopped") }
            .onFailure {
                when (it) {
                    is BackendException.StateConflict ->
                        call.respond(HttpStatusCode.Conflict, it.message)
                    else ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to stop tunnel $id due to internal error.",
                        )
                }
            }
    }
}

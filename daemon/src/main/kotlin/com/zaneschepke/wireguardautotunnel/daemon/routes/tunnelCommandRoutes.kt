// 3. Updated tunnelCommandRoutes.kt (no TunnelRepository dependency)
package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.StartTunnelRequest
import com.zaneschepke.wireguardautotunnel.daemon.dto.toDto
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import com.zaneschepke.wireguardautotunnel.daemon.util.unwrapVerifiedPayload
import com.zaneschepke.wireguardautotunnel.daemon.util.verifiedPayload
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

fun Route.tunnelCommandRoutes(json: Json, backend: Backend) {

    val logger = Logger.withTag("TunnelCommands")

    // START TUNNEL
    post("/start") {
        val request = call.unwrapVerifiedPayload<StartTunnelRequest>(
            json = json,
            logger = logger,
            logMessage = "Failed to parse start request"
        ) ?: return@post

        logger.i { "Starting tunnel ${request.id} (${request.name})" }

        val tunnel = RunningTunnel(request.id, request.name)

        val result = backend.start(tunnel, request.quickConfig)

        if (result.isFailure) {
            logger.e(result.exceptionOrNull()) { "Failed to start tunnel ${request.id}" }
            call.respond(HttpStatusCode.InternalServerError, "Failed to start tunnel")
        } else {
            call.respond(HttpStatusCode.OK, "Tunnel ${request.id} started")
        }
    }

    // STOP TUNNEL
    post("/stop/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")

        logger.i { "Stopping tunnel $id" }

        backend.stop(id)

        call.respond(HttpStatusCode.OK, "Tunnel $id stopped")
    }

//    post("/mode") {
//        val mode = call.unwrapVerifiedPayload<BackendMode>(
//            json = json,
//            logger = logger,
//            logMessage = "Failed to parse mode request"
//        ) ?: return@post
//
//        logger.i { "Setting backend mode to $mode" }
//        backend.setMode(mode)
//        call.respond(HttpStatusCode.OK, "Mode set to $mode")
//    }
//
//    post("/kill-switch") {
//        val enabledStr = call.verifiedPayload()?.trim()
//            ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing enabled value")
//
//        val enabled = enabledStr.equals("true", ignoreCase = true)
//
//        logger.i { "Setting kill switch to $enabled" }
//        val result = backend.setKillSwitch(enabled)
//        if (result.isFailure) {
//            call.respond(HttpStatusCode.InternalServerError, "Failed to toggle kill switch")
//        } else {
//            call.respond(HttpStatusCode.OK, "Kill switch set to $enabled")
//        }
//    }
//
//    get("/status") {
//        val status = backend.status.first()
//        call.respond(HttpStatusCode.OK, status.toDto())
//    }
//
//    webSocket("/status/stream") {
//        logger.i { "Client connected to /tunnel/status/stream" }
//        try {
//            backend.status
//                .map { it.toDto() }
//                .collect { dto ->
//                    val text = json.encodeToString(dto)
//                    send(Frame.Text(text))
//                }
//        } catch (e: Exception) {
//            logger.e(e) { "Error streaming status" }
//        } finally {
//            logger.i { "Client disconnected from status stream" }
//        }
//    }
}
package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.wgtunnel.backend.exception.BackendException
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.KillSwitchRequest
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.dto.toDto
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val log = Logger.withTag("BackendRoutes")

fun Route.backendRoutes(backend: Backend, cacheRepository: DaemonCacheRepository) {

    put(Routes.BACKEND_KILL_SWITCH) {
        val request = call.receive<KillSwitchRequest>()
        log.i { "Setting kill switch enabled=${request.enabled}" }

        val result =
            if (request.enabled) {
                val config =
                    request.config?.toCore()
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            "Kill switch config is required when enabling",
                        )
                backend.setKillSwitch(config)
            } else {
                backend.disableKillSwitch()
            }

        result
            .onSuccess {
                cacheRepository.updateKillSwitchEnabled(request.enabled)
                cacheRepository.updateKillSwitchConfig(request.config)
                call.respond(
                    HttpStatusCode.OK,
                    "Kill switch set to ${request.enabled} successfully",
                )
            }
            .onFailure { error ->
                log.e(error) { "Failed to set kill switch" }
                when (error) {
                    is BackendException ->
                        call.respond(HttpStatusCode.BadRequest, error.message ?: "Backend error")
                    else ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to toggle kill switch",
                        )
                }
            }
    }

    get(Routes.BACKEND_STATUS) {
        val status = backend.status.first()
        call.respond(HttpStatusCode.OK, status.toDto())
    }

    webSocket(Routes.BACKEND_STATUS_WS) {
        log.i { "Client connected to backend status stream" }
        try {
            backend.status
                .map { it.toDto() }
                .distinctUntilChanged()
                .collect { dto -> sendSerialized(dto) }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                log.e(e) { "Error streaming status" }
            }
        }
    }
}

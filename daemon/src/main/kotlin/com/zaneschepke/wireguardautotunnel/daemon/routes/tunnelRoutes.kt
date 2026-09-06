package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.wgtunnel.backend.exception.BackendException
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.dto.toBackendMode
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val log = Logger.withTag("TunnelRoutes")

fun Route.tunnelRoutes(
    backend: Backend,
    daemonCacheRepository: DaemonCacheRepository,
    autoTunnelSupervisor: AutoTunnelSupervisor,
) {
    val tunnelOperationMutex = Mutex()

    post(Routes.Tunnels.START_TEMPLATE) {
        tunnelOperationMutex.withLock {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")
            autoTunnelSupervisor.notifyUserOverride()
            val request = call.receive<StartTunnelRequest>()

            log.i { "Starting tunnel ${request.name} (id=$id, mode=${request.mode})" }

            val config =
                try {
                    Config.parseQuickString(request.quickConfig)
                } catch (e: Exception) {
                    log.e(e) { "Failed to parse tunnel config" }
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Invalid tunnel configuration",
                    )
                }

            val tunnel = RunningTunnel.fromRequest(id, request)
            val mode = request.toBackendMode(config)
            val dns = request.tunnelDns?.toCore()

            daemonCacheRepository.updateLastStartRequest(id.toLong(), request)

            backend
                .start(tunnel = tunnel, mode = mode, tunnelDnsConfig = dns)
                .onSuccess { call.respond(HttpStatusCode.OK, "Tunnel ${request.name} started") }
                .onFailure { error ->
                    log.e(error) { "Failed to start tunnel ${request.name}" }
                    when (error) {
                        is BackendException ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                error.message ?: "Backend error starting tunnel",
                            )
                        else ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                error.message
                                    ?: "Failed to start tunnel ${request.name} due to internal error.",
                            )
                    }
                }
        }
    }

    post(Routes.Tunnels.STOP_TEMPLATE) {
        tunnelOperationMutex.withLock {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid id")
            autoTunnelSupervisor.notifyUserOverride()

            backend
                .stop(id)
                .onSuccess { call.respond(HttpStatusCode.OK, "Tunnel $id stopped") }
                .onFailure { error ->
                    log.e(error) { "Failed to stop tunnel $id" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        error.message ?: "Failed to stop tunnel $id due to internal error.",
                    )
                }
        }
    }
}

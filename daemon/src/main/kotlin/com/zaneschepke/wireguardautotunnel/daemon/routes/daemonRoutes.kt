package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.awaitCancellation

fun Route.daemonRoutes(daemonCacheRepository: DaemonCacheRepository) {
    get(Routes.DAEMON_STATUS) { call.response.status(HttpStatusCode.OK) }
    webSocket(Routes.DAEMON_STATUS_WS) {
        try {
            awaitCancellation()
        } finally {}
    }

    put(Routes.DAEMON_RESTORE_TUNNEL) {
        val request = call.receive<FlagRequest>()
        Logger.d { "Updating restore tunnel to ${request.value}" }
        daemonCacheRepository.setRestoreTunnelOnBoot(request.value)
        Logger.d { "Successfully updated restore tunnel to ${request.value}" }
        call.respond(HttpStatusCode.OK, "Tunnel restore updated to ${request.value}")
    }

    put(Routes.DAEMON_RESTORE_KILL_SWITCH) {
        val request = call.receive<FlagRequest>()
        Logger.d { "Updating restore kill switch to ${request.value}" }
        daemonCacheRepository.updateKillSwitchRestore(request.value)
        Logger.d { "Successfully updated restore kill switch to ${request.value}" }
        call.respond(HttpStatusCode.OK, "Kill switch restore updated to ${request.value}")
    }
}

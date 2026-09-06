package com.zaneschepke.wireguardautotunnel.daemon.routes

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
import com.zaneschepke.wireguardautotunnel.daemon.autotunnel.AutoTunnelSupervisor
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.log.DaemonLogService
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.io.File
import kotlinx.coroutines.awaitCancellation

private val log = Logger.withTag("DaemonRoutes")

fun Route.daemonRoutes(
    daemonCacheRepository: DaemonCacheRepository,
    autoTunnelSupervisor: AutoTunnelSupervisor,
    daemonLogService: DaemonLogService,
) {
    get(Routes.DAEMON_STATUS) { call.response.status(HttpStatusCode.OK) }
    webSocket(Routes.DAEMON_STATUS_WS) {
        try {
            awaitCancellation()
        } finally {}
    }

    put(Routes.DAEMON_RESTORE_TUNNEL) {
        val request = call.receive<FlagRequest>()
        log.d { "Updating restore tunnel to ${request.value}" }
        daemonCacheRepository.setRestoreTunnelOnBoot(request.value)
        log.d { "Successfully updated restore tunnel to ${request.value}" }
        call.respond(HttpStatusCode.OK, "Tunnel restore updated to ${request.value}")
    }

    put(Routes.DAEMON_RESTORE_KILL_SWITCH) {
        val request = call.receive<FlagRequest>()
        log.d { "Updating restore kill switch to ${request.value}" }
        daemonCacheRepository.updateKillSwitchRestore(request.value)
        log.d { "Successfully updated restore kill switch to ${request.value}" }
        call.respond(HttpStatusCode.OK, "Kill switch restore updated to ${request.value}")
    }

    put(Routes.DAEMON_AUTO_TUNNEL_PLAN) {
        val plan = call.receive<AutoTunnelConfigDto>()
        log.d {
            "Updating auto-tunnel plan enabled=${plan.enabled} startOnBoot=${plan.startOnBoot} tunnels=${plan.tunnels.size}"
        }
        autoTunnelSupervisor.updatePlan(plan)
        call.respond(HttpStatusCode.OK, "Auto-tunnel plan updated")
    }

    get(Routes.DAEMON_AUTO_TUNNEL_STATUS) {
        call.respond(HttpStatusCode.OK, autoTunnelSupervisor.status)
    }

    webSocket(Routes.DAEMON_AUTO_TUNNEL_STATUS_WS) {
        try {
            autoTunnelSupervisor.statusFlow.collect { sendSerialized(it) }
        } finally {}
    }

    post(Routes.DAEMON_AUTO_TUNNEL_OVERRIDE) {
        autoTunnelSupervisor.notifyUserOverride()
        call.respond(HttpStatusCode.OK, "Auto-tunnel user override recorded")
    }

    put(Routes.DAEMON_LOGS_ENABLED) {
        val request = call.receive<FlagRequest>()
        daemonLogService.setEnabled(request.value)
        call.respond(HttpStatusCode.OK, "Local logging ${request.value}")
    }

    webSocket(Routes.DAEMON_LOGS_WS) {
        try {
            daemonLogService.messages.collect { sendSerialized(it) }
        } finally {}
    }

    post(Routes.DAEMON_LOGS_CLEAR) {
        daemonLogService.clear()
        call.respond(HttpStatusCode.OK, "Logs cleared")
    }

    get(Routes.DAEMON_LOGS_ZIP) {
        val zip = File.createTempFile("wgtunnel-daemon-logs", ".zip")
        try {
            daemonLogService.zipTo(zip)
            call.respondFile(zip)
        } finally {
            zip.delete()
        }
    }
}

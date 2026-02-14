package com.zaneschepke.wireguardautotunnel.daemon.routes

import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.awaitCancellation

fun Route.daemonRoutes() {
    get(Routes.DAEMON_STATUS) { call.response.status(HttpStatusCode.OK) }
    webSocket(Routes.DAEMON_STATUS_WS) {
        try {
            awaitCancellation()
        } finally {}
    }
}

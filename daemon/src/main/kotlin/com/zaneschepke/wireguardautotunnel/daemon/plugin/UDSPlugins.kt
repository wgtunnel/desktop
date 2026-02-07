package com.zaneschepke.wireguardautotunnel.daemon.plugin

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.ipc.Headers
import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

val hmacShieldPlugin = createApplicationPlugin("HmacShield") {
    onCall { call ->

        // ignore daemon routes
        if (call.request.path().contains(Routes.DAEMON_BASE)) {
            return@onCall
        }

        // ignore websocket upgrade
        if (call.request.headers[HttpHeaders.Upgrade]?.equals("websocket", ignoreCase = true) == true) {
            Logger.d { "Daemon: Allowing WebSocket handshake for ${call.request.path()}" }
            return@onCall
        }

        val userHint = call.request.headers[Headers.HMAC_USER]
        val timestamp = call.request.headers[Headers.HMAC_TIMESTAMP]?.toLong() ?: 0L
        val signature = call.request.headers[Headers.HMAC_SIGNATURE]

        if (userHint == null || signature == null) {
            Logger.w { "Daemon: Rejecting request - Missing Headers" }
            return@onCall call.respond(HttpStatusCode.Unauthorized, "Identity headers missing")
        }

        val secret = IPC.resolveKeyForUser(userHint)
            ?: return@onCall call.respond(HttpStatusCode.Unauthorized, "User not recognized")

        val bodyText = call.receiveText()

        if (!HmacProtector.verify(secret, timestamp, signature, bodyText)) {
            Logger.e { "Daemon: HMAC Mismatch! Path: ${call.request.path()} Body: '$bodyText'" }
            return@onCall call.respond(HttpStatusCode.Unauthorized, "Invalid HMAC")
        }

        Logger.d { "Daemon: HMAC Verified for ${call.request.path()}" }
    }
}
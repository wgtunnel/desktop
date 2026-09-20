package com.zaneschepke.wireguardautotunnel.daemon.plugin

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.ipc.Headers
import com.zaneschepke.wireguardautotunnel.core.ipc.IpcKeyFileValidator
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

private val log = Logger.withTag("HmacShield")

val hmacShieldPlugin =
    createApplicationPlugin("HmacShield") {
        onCall { call ->

            // The alive check doesn't need protection
            if (call.request.path() == Routes.DAEMON_STATUS) {
                return@onCall
            }

            // WebSocket upgrades never carry a body so they signed/verified below against an empty
            // payload
            val isWebSocketUpgrade =
                call.request.headers[HttpHeaders.Upgrade]?.equals("websocket", ignoreCase = true) ==
                    true

            val keyPathStr =
                call.request.headers[Headers.HMAC_KEY_PATH]
                    ?: return@onCall call.respond(
                        HttpStatusCode.Unauthorized,
                        "Missing IPC key path",
                    )

            val secret =
                when (val result = IpcKeyFileValidator.resolve(keyPathStr)) {
                    is IpcKeyFileValidator.Result.Rejected -> {
                        log.e { "Daemon: ${result.reason}" }
                        return@onCall call.respond(HttpStatusCode.Unauthorized, result.reason)
                    }
                    is IpcKeyFileValidator.Result.Trusted -> result.secret
                }

            val timestamp = call.request.headers[Headers.HMAC_TIMESTAMP]?.toLong() ?: 0L
            val signature = call.request.headers[Headers.HMAC_SIGNATURE]

            if (signature == null) {
                log.w { "Daemon: Missing HMAC signature" }
                return@onCall call.respond(HttpStatusCode.Unauthorized, "Missing signature")
            }

            val bodyText = if (isWebSocketUpgrade) "" else call.receiveText()

            if (!HmacProtector.verify(secret, timestamp, signature, bodyText)) {
                log.e { "Daemon: HMAC Mismatch! Path: ${call.request.path()}" }
                return@onCall call.respond(HttpStatusCode.Unauthorized, "Invalid HMAC")
            }
        }
    }

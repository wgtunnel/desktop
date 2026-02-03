package com.zaneschepke.wireguardautotunnel.daemon.plugin

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.SecureCommand
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey

object UDSPlugins {

    const val VERIFIED_PAYLOAD_KEY = "verifiedPayload"

    val hmacShieldPlugin = createRouteScopedPlugin("HmacShield") {
        val payloadKey = AttributeKey<String>(VERIFIED_PAYLOAD_KEY)

        onCall { call ->
            try {
                Logger.d { "Verifying request..." }
                val command = call.receive<SecureCommand>()

                Logger.d { "Resolving users secret..." }
                val secret = IPC.resolveKeyForUser(command.userHint)
                    ?: return@onCall call.respond(HttpStatusCode.Unauthorized, "Unable to resolve key for user")

                Logger.d { "Verifying users secret..." }
                if (!HmacProtector.verify(secret, command)) {
                    Logger.e { "Invalid user secret. Unauthorized request." }
                    call.respond(HttpStatusCode.Unauthorized, "Invalid HMAC Handshake")
                    return@onCall
                }
                Logger.d { "Constructing verified payload..." }
                command.payload?.let { call.attributes.put(payloadKey, it) }

            } catch (e: Exception) {
                Logger.e(e){ "Invalid user secret. Unauthorized request." }
                call.respond(HttpStatusCode.BadRequest, "Secure envelope missing or malformed: ${e.message}")
            }
        }
    }
}
package com.zaneschepke.wireguardautotunnel.daemon.plugin

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.Headers
import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.nio.file.Paths

private val log = Logger.withTag("HmacShield")

val hmacShieldPlugin =
    createApplicationPlugin("HmacShield") {
        onCall { call ->

            // ignore daemon health calls
            if (call.request.path() == Routes.DAEMON_BASE) {
                return@onCall
            }

            // ignore websocket upgrade
            if (
                call.request.headers[HttpHeaders.Upgrade]?.equals("websocket", ignoreCase = true) ==
                    true
            ) {
                log.d { "Daemon: Allowing WebSocket handshake for ${call.request.path()}" }
                return@onCall
            }

            val keyPathStr =
                call.request.headers[Headers.HMAC_KEY_PATH]
                    ?: return@onCall call.respond(
                        HttpStatusCode.Unauthorized,
                        "Missing IPC key path",
                    )

            val keyFile =
                try {
                    Paths.get(keyPathStr).normalize().toAbsolutePath().toFile()
                } catch (e: Exception) {
                    log.w { "Daemon: Invalid path format: $keyPathStr" }
                    return@onCall call.respond(HttpStatusCode.Unauthorized, "Invalid key path")
                }

            if (
                keyFile.name != IPC.KEY_FILE ||
                    keyFile.parentFile?.name != AppVariant.current.ipcFolder
            ) {
                log.w { "Daemon: Path does not match expected structure: ${keyFile.absolutePath}" }
                return@onCall call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid key path structure",
                )
            }
            if (!keyFile.isFile) {
                log.w { "Daemon: Key file does not exist: ${keyFile.absolutePath}" }
                return@onCall call.respond(HttpStatusCode.Unauthorized, "Key file not found")
            }

            // ensure it is user only owned, as we expect
            if (!PermissionsHelper.isOwnerOnly(keyFile.toPath())) {
                log.e { "Daemon: Key file permissions are not 0600: ${keyFile.absolutePath}" }
                return@onCall call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid key file permissions",
                )
            }

            val secret =
                keyFile.readText().trim().takeIf { it.isNotBlank() }
                    ?: return@onCall call.respond(HttpStatusCode.Unauthorized, "Empty key file")

            val timestamp = call.request.headers[Headers.HMAC_TIMESTAMP]?.toLong() ?: 0L
            val signature = call.request.headers[Headers.HMAC_SIGNATURE]

            if (signature == null) {
                log.w { "Daemon: Missing HMAC signature" }
                return@onCall call.respond(HttpStatusCode.Unauthorized, "Missing signature")
            }

            val bodyText = call.receiveText()

            if (!HmacProtector.verify(secret, timestamp, signature, bodyText)) {
                log.e { "Daemon: HMAC Mismatch! Path: ${call.request.path()} Body: '$bodyText'" }
                return@onCall call.respond(HttpStatusCode.Unauthorized, "Invalid HMAC")
            }
        }
    }

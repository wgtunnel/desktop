package com.zaneschepke.wireguardautotunnel.daemon.util

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.daemon.plugin.UDSPlugins
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

inline fun <reified T> ApplicationCall.verifiedPayload(json: Json): Result<T> {
    val payloadStr = attributes.getOrNull(AttributeKey<String>(UDSPlugins.VERIFIED_PAYLOAD_KEY))
        ?: return Result.failure(IllegalArgumentException("Missing payload"))

    return try {
        Result.success(json.decodeFromString<T>(payloadStr))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend inline fun <reified T> ApplicationCall.unwrapVerifiedPayload(
    json: Json,
    logger: Logger,
    logMessage: String = "Failed to parse payload",
    errorResponseMessage: String = "Invalid JSON payload"
): T? {
    val result = verifiedPayload<T>(json)
    if (result.isFailure) {
        logger.e(result.exceptionOrNull()) { logMessage }
        respond(HttpStatusCode.BadRequest, errorResponseMessage)
        return null
    }
    return result.getOrThrow()
}
package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

suspend fun <T> safeDaemonCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: ClientException) {
        // mapped by validator
        Result.failure(e)
    } catch (_: IOException) {
        Result.failure(ClientException.DaemonCommsException())
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}

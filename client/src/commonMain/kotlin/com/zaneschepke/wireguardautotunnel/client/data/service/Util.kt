package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeDaemonCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: ClientException) {
        // mapped by validator
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(ClientException.DaemonCommsException())
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}
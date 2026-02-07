package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsDaemonHealthService.Companion.DAEMON_WS_RECONNECT_DELAY_MILLIS
import com.zaneschepke.wireguardautotunnel.client.service.BackendCommandService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.KillSwitchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class UdsBackendCommandService(
    private val client: HttpClient,
    private val json: Json
) : BackendCommandService {

    override suspend fun setMode(mode: BackendMode): Result<Unit> = safeDaemonCall {
        client.post(Routes.BACKEND_MODE) {
            setBody(mode)
        }
    }


    override suspend fun setKillSwitch(enabled: Boolean): Result<Unit> = safeDaemonCall {
        val request = KillSwitchRequest(enabled)
        client.post(Routes.BACKEND_KILL_SWITCH) {
            setBody(request)
        }
    }

    override suspend fun getStatus(): Result<BackendStatus> = runCatching {
        val response = client.get(Routes.BACKEND_STATUS)
        response.body<BackendStatus>()
    }

    override fun statusFlow(): Flow<BackendStatus> = callbackFlow {
        var initialSent = false

        while (isActive) {
            try {
                if (!initialSent) {
                    getStatus().onSuccess { trySend(it) }
                    initialSent = true
                }

                client.webSocket(path = Routes.BACKEND_STATUS_WS) {
                    Logger.d { "Client: WS Connected" }
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val status = json.decodeFromString<BackendStatus>(text)
                            trySend(status)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                delay(DAEMON_WS_RECONNECT_DELAY_MILLIS)
            }
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)
}
package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.service.DaemonHealthService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class UdsDaemonHealthService(private val client: HttpClient) : DaemonHealthService {

    override suspend fun alive(): Boolean {
        return try {
            client.get(Routes.DAEMON_STATUS).status.isSuccess()
        } catch (e: Exception) {
            Logger.w(e) { "UDS Daemon service not available" }
            false
        }
    }

    override val alive: Flow<Boolean> =
        callbackFlow {
                while (isActive) {
                    try {
                        client.webSocket(Routes.DAEMON_STATUS_WS) {
                            send(true)
                            try {
                                // suspend until socket closed by server
                                incoming.receive()
                            } catch (_: Exception) {
                                Logger.w { "Socket closed by server" }
                                trySend(false)
                            }
                        }
                    } catch (_: Exception) {
                        trySend(false)
                        delay(DAEMON_WS_RECONNECT_DELAY_MILLIS)
                    }
                }
                awaitClose {}
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    companion object {
        const val DAEMON_WS_RECONNECT_DELAY_MILLIS = 3_000L
    }
}

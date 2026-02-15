package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
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

class UdsDaemonService(
    private val client: HttpClient,
    private val lockdownSettingsRepository: LockdownSettingsRepository,
    private val generalSettingsRepository: GeneralSettingRepository,
) : DaemonService {

    override suspend fun alive(): Boolean {
        return try {
            client.get(Routes.DAEMON_STATUS).status.isSuccess()
        } catch (e: Exception) {
            Logger.w(e) { "UDS Daemon service not available" }
            false
        }
    }

    override suspend fun setRestoreKillSwitch(enabled: Boolean): Result<Unit> {
        lockdownSettingsRepository.updateRestoreOnBoot(enabled)
        return safeDaemonCall {
                val request = FlagRequest(enabled)
                client.put(Routes.DAEMON_RESTORE_KILL_SWITCH) { setBody(request) }
                Unit
            }
            .onFailure { lockdownSettingsRepository.updateRestoreOnBoot(!enabled) }
    }

    override suspend fun setRestoreTunnel(enabled: Boolean): Result<Unit> {
        generalSettingsRepository.updateRestoreTunnelOnBoot(enabled)
        return safeDaemonCall {
                val request = FlagRequest(enabled)
                client.put(Routes.DAEMON_RESTORE_TUNNEL) { setBody(request) }
                Unit
            }
            .onFailure { generalSettingsRepository.updateRestoreTunnelOnBoot(!enabled) }
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

package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsDaemonService.Companion.DAEMON_WS_RECONNECT_DELAY_MILLIS
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
import com.zaneschepke.wireguardautotunnel.parser.ActiveConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class UdsBackendService(
    private val client: HttpClient,
    private val json: Json,
    private val lockdownSettingsRepository: LockdownSettingsRepository,
) : BackendService {

    override suspend fun setMode(mode: BackendMode): Result<Unit> = safeDaemonCall {
        client.put(Routes.BACKEND_MODE) { setBody(mode) }
    }

    override suspend fun setKillSwitch(enabled: Boolean): Result<Unit> {
        lockdownSettingsRepository.updateEnabled(enabled)
        return safeDaemonCall {
                val request = FlagRequest(enabled)
                client.put(Routes.BACKEND_KILL_SWITCH) { setBody(request) }
                Unit
            }
            .onFailure { lockdownSettingsRepository.updateEnabled(!enabled) }
    }

    override suspend fun setKillSwitchLanBypass(enabled: Boolean): Result<Unit> {
        lockdownSettingsRepository.updateBypassLan(enabled)
        return safeDaemonCall {
                val request = FlagRequest(enabled)
                client.put(Routes.BACKEND_KILL_SWITCH_BYPASS) { setBody(request) }
                Unit
            }
            .onFailure { lockdownSettingsRepository.updateBypassLan(!enabled) }
    }

    override suspend fun getStatus(): Result<BackendStatus> = runCatching {
        val response = client.get(Routes.BACKEND_STATUS)
        response.body<BackendStatus>()
    }

    private suspend fun getActiveConfig(id: Long): Result<String?> = runCatching {
        val response = client.get(Routes.BACKEND_ACTIVE_CONFIG.replace("{id}", id.toString()))
        if (response.status == HttpStatusCode.OK) {
            response.body<String>()
        } else {
            null
        }
    }

    private suspend fun enrichWithConfigs(basicStatus: BackendStatus): BackendStatus {
        val updatedTunnels =
            basicStatus.activeTunnels.map { tunnelStatus ->
                if (tunnelStatus.state != TunnelState.DOWN) {
                    val configResult = getActiveConfig(tunnelStatus.id)
                    val activeConfig =
                        configResult.getOrNull()?.let { str ->
                            try {
                                ActiveConfig.parseFromIpc(str)
                            } catch (e: Exception) {
                                Logger.e(e) {
                                    "Failed to parse active config for tunnel ${tunnelStatus.id}"
                                }
                                null
                            }
                        }
                    tunnelStatus.copy(activeConfig = activeConfig)
                } else {
                    tunnelStatus.copy(activeConfig = null)
                }
            }
        return basicStatus.copy(activeTunnels = updatedTunnels)
    }

    private fun basicStatusFlow(): Flow<BackendStatus> =
        callbackFlow {
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

                awaitClose {}
            }
            .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun statusFlow(): Flow<BackendStatus> =
        basicStatusFlow()
            .flatMapLatest { basic ->
                flow {
                    emit(enrichWithConfigs(basic))
                    while (true) {
                        delay(3000)
                        emit(enrichWithConfigs(basic))
                    }
                }
            }
            .flowOn(Dispatchers.IO)
}

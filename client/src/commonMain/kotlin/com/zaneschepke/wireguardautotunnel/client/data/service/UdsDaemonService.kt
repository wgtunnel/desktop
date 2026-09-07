package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.FlagRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class UdsDaemonService(
    private val client: HttpClient,
    private val lockdownSettingsRepository: LockdownSettingsRepository,
    private val generalSettingsRepository: GeneralSettingRepository,
    private val json: Json,
    scope: CoroutineScope,
) : DaemonService {

    override val alive: Flow<Boolean> = callbackFlow {
        var failureCount = 0
        while (isActive) {
            var connected = false
            try {
                client.webSocket(Routes.DAEMON_STATUS_WS) {
                    connected = true
                    trySend(true)
                    for (frame in incoming) {
                        // Keep the session open, ends when socket closes
                    }
                    trySend(false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                trySend(false)
            }
            failureCount = if (connected) 0 else failureCount + 1
            if (isActive) reconnectDelay(failureCount)
        }
        awaitClose {}
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    private val autoTunnelStatus: Flow<AutoTunnelStatusDto> = callbackFlow {
        var failureCount = 0
        while (isActive) {
            var connected = false
            try {
                getAutoTunnelStatus().onSuccess {
                    connected = true
                    trySend(it)
                }
                client.webSocket(path = Routes.DAEMON_AUTO_TUNNEL_STATUS_WS) {
                    connected = true
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            trySend(
                                json.decodeFromString(
                                    AutoTunnelStatusDto.serializer(),
                                    frame.readText(),
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            failureCount = if (connected) 0 else failureCount + 1
            if (isActive) reconnectDelay(failureCount)
        }
        awaitClose {}
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    private val logs: Flow<LogMessageDto> = callbackFlow {
        var failureCount = 0
        while (isActive) {
            var connected = false
            try {
                client.webSocket(path = Routes.DAEMON_LOGS_WS) {
                    connected = true
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            trySend(
                                json.decodeFromString(
                                    LogMessageDto.serializer(),
                                    frame.readText(),
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            failureCount = if (connected) 0 else failureCount + 1
            if (isActive) reconnectDelay(failureCount)
        }
        awaitClose {}
    }
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 0)

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

    override suspend fun updateAutoTunnelConfig(plan: AutoTunnelConfigDto): Result<Unit> =
        safeDaemonCall {
            client.put(Routes.DAEMON_AUTO_TUNNEL_PLAN) { setBody(plan) }.status
        }

    override suspend fun getAutoTunnelStatus(): Result<AutoTunnelStatusDto> = safeDaemonCall {
        client.get(Routes.DAEMON_AUTO_TUNNEL_STATUS).body()
    }

    override fun autoTunnelStatusFlow(): Flow<AutoTunnelStatusDto> = autoTunnelStatus

    override suspend fun setLocalLogging(enabled: Boolean): Result<Unit> = safeDaemonCall {
        client.put(Routes.DAEMON_LOGS_ENABLED) { setBody(FlagRequest(enabled)) }.status
    }

    override suspend fun clearLogs(): Result<Unit> = safeDaemonCall {
        client.post(Routes.DAEMON_LOGS_CLEAR)
    }

    override suspend fun downloadLogZip(): Result<ByteArray> = safeDaemonCall {
        client.get(Routes.DAEMON_LOGS_ZIP).body()
    }

    override fun logsFlow(): Flow<LogMessageDto> = logs

    companion object {
        const val DAEMON_WS_RECONNECT_DELAY_MILLIS = 3_000L
        private const val INITIAL_RECONNECT_DELAY_MILLIS = 200L
        private const val SHARE_STOP_TIMEOUT_MS = 5_000L

        /**
         * Bounded exponential backoff for daemon (re)connect attempts. Fresh subscriptions and
         * connections that just dropped retry almost immediately; only repeated failures back off
         * toward [DAEMON_WS_RECONNECT_DELAY_MILLIS]. Keeps a cold app open from stalling a full 3s
         * on a single transient first-attempt failure.
         */
        suspend fun reconnectDelay(failureCount: Int) {
            val delayMillis =
                (INITIAL_RECONNECT_DELAY_MILLIS shl failureCount.coerceAtMost(4)).coerceAtMost(
                    DAEMON_WS_RECONNECT_DELAY_MILLIS
                )
            delay(delayMillis)
        }
    }
}

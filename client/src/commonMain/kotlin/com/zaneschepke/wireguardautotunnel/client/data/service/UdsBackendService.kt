package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsDaemonService.Companion.DAEMON_WS_RECONNECT_DELAY_MILLIS
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.KillSwitchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class UdsBackendService(
    private val client: HttpClient,
    private val json: Json,
    private val lockdownSettingsRepository: LockdownSettingsRepository,
    private val daemonService: DaemonService,
    scope: CoroutineScope,
) : BackendService {

    override suspend fun setKillSwitch(
        enabled: Boolean,
        config: KillSwitchConfigDto?,
    ): Result<Unit> {
        lockdownSettingsRepository.updateEnabled(enabled)
        if (!enabled) {
            val settings = lockdownSettingsRepository.get()
            if (settings.restoreOnBoot) {
                daemonService.setRestoreKillSwitch(false)
            }
        }

        val resolvedConfig =
            config
                ?: if (enabled) {
                    lockdownSettingsRepository.get().toDto()
                } else null

        return safeDaemonCall {
            client.put(Routes.BACKEND_KILL_SWITCH) {
                setBody(KillSwitchRequest(enabled = enabled, config = resolvedConfig))
            }
            Unit
        }
            .onFailure { lockdownSettingsRepository.updateEnabled(!enabled) }
    }

    override suspend fun getStatus(): Result<BackendStatus> = runCatching {
        val response = client.get(Routes.BACKEND_STATUS)
        response.body<BackendStatus>()
    }

    override fun statusFlow(): Flow<BackendStatus> = status

    private val status: Flow<BackendStatus> = callbackFlow {
        while (isActive) {
            try {
                getStatus().onSuccess { trySend(it) }
                client.webSocket(path = Routes.BACKEND_STATUS_WS) {
                    Logger.d { "Client: WS Connected" }
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val parsed = json.decodeFromString<BackendStatus>(text)
                            trySend(parsed)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            if (isActive) delay(DAEMON_WS_RECONNECT_DELAY_MILLIS.milliseconds)
        }

        awaitClose {}
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
}

package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.StartTunnelRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okio.IOException

class UdsTunnelCommandService(
    private val client: HttpClient,
    private val tunnelRepository: TunnelRepository
) : TunnelCommandService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun startTunnel(id: Int): Result<Unit> = runCatching {
        val tunnelConfig = tunnelRepository.getById(id)
            ?: throw IOException("Tunnel $id not found")

        val request = StartTunnelRequest(
            id = id,
            name = tunnelConfig.name,
            quickConfig = tunnelConfig.quickConfig
        )

        val response = client.post("/tunnel/start") {
            setBody(json.encodeToString(request))
            contentType(ContentType.Application.Json)
        }

        if (!response.status.isSuccess()) {
            throw IOException("Failed to start tunnel $id: ${response.status.value} - ${response.bodyAsText()}")
        }
    }

    override suspend fun stopTunnel(id: Int): Result<Unit> = runCatching {
        val response = client.post("/tunnel/stop/$id")

        if (!response.status.isSuccess()) {
            throw IOException("Failed to stop tunnel $id: ${response.status.value} - ${response.bodyAsText()}")
        }
    }

    override suspend fun setMode(mode: BackendMode): Result<Unit> = runCatching {
        val response = client.post("/tunnel/mode") {
            setBody(json.encodeToString(mode))
            contentType(ContentType.Text.Plain)
        }

        if (!response.status.isSuccess()) {
            throw IOException("Failed to set mode: ${response.bodyAsText()}")
        }
    }

    override suspend fun setKillSwitch(enabled: Boolean): Result<Unit> = runCatching {
        val response = client.post("/tunnel/kill-switch") {
            setBody(enabled.toString())
            contentType(ContentType.Text.Plain)
        }

        if (!response.status.isSuccess()) {
            throw IOException("Failed to set kill switch: ${response.bodyAsText()}")
        }
    }

    override suspend fun getStatus(): Result<BackendStatus> = runCatching {
        val response = client.get("/tunnel/status")

        if (!response.status.isSuccess()) {
            throw IOException("Failed to get status: ${response.status.value} - ${response.bodyAsText()}")
        }

        response.body<BackendStatus>()
    }

    override fun statusFlow(): Flow<BackendStatus> = callbackFlow {
        val session = client.webSocketSession("/tunnel/status/stream")

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val dto = json.decodeFromString<BackendStatus>(frame.readText())
                    trySend(dto)
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            session.close()
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)
}
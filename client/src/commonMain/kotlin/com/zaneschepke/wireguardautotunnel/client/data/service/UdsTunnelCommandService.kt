package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.error.BackendError
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class UdsTunnelCommandService(
    private val client: HttpClient,
    private val tunnelRepository: TunnelRepository,
) : TunnelCommandService {

    override suspend fun startTunnel(id: Long): Result<Unit> = safeDaemonCall {
        val tunnelConfig = tunnelRepository.getById(id)
            ?: throw ClientException.BackendException(BackendError.StartTunnel.NotFound)

        val request = StartTunnelRequest(
            name = tunnelConfig.name,
            quickConfig = tunnelConfig.quickConfig
        )

        client.post(Routes.Tunnels.start(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun stopTunnel(id: Long): Result<Unit> = safeDaemonCall {
        client.post(Routes.Tunnels.stop(id))
    }
}
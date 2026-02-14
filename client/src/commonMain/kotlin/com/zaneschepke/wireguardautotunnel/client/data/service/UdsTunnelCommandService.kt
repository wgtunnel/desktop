package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UdsTunnelCommandService(
    private val client: HttpClient,
    private val tunnelRepository: TunnelRepository,
) : TunnelCommandService {

    val mutex = Mutex()

    override suspend fun startTunnel(id: Long): Result<Unit> =
        mutex.withLock {
            val tunnelConfig =
                tunnelRepository.getById(id)
                    ?: throw ClientException.BadRequestException(
                        "Could not find tunnel with id=$id"
                    )

            tunnelRepository.save(tunnelConfig.copy(active = true))

            return safeDaemonCall {
                    val request =
                        StartTunnelRequest(
                            name = tunnelConfig.name,
                            quickConfig = tunnelConfig.quickConfig,
                        )

                    client.post(Routes.Tunnels.start(id)) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                    Unit
                }
                .onFailure { _ ->
                    // reset db on failure
                    tunnelRepository.save(tunnelConfig.copy(active = false))
                }
        }

    override suspend fun stopTunnel(id: Long): Result<Unit> =
        mutex.withLock {
            return safeDaemonCall {
                val tunnelConfig =
                    tunnelRepository.getById(id)
                        ?: throw ClientException.BadRequestException(
                            "Could not find tunnel with id=$id"
                        )

                tunnelRepository.save(tunnelConfig.copy(active = false))
                client.post(Routes.Tunnels.stop(id))
            }
        }
}

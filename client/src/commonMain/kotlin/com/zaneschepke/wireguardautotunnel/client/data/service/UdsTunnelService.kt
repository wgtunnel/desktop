package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UdsTunnelService(
    private val client: HttpClient,
    private val tunnelRepository: TunnelRepository,
) : TunnelService {

    private val tunnelCommandMutex = Mutex()

    override suspend fun startTunnel(id: Long): Result<Unit> =
        tunnelCommandMutex.withLock {
            val newTunnel =
                tunnelRepository.getById(id)
                    ?: return@withLock Result.failure(
                        ClientException.BadRequestException("Could not find tunnel with id=$id")
                    )

            // stop active tunnels to enforce single active
            val previouslyActive = tunnelRepository.getActive().filter { it.id != id }
            previouslyActive.forEach { oldTunnel ->
                tunnelRepository.save(oldTunnel.copy(active = false))

                safeDaemonCall { client.post(Routes.Tunnels.stop(oldTunnel.id)) }
                    .onFailure {
                        Logger.w(it) {
                            "Failed to stop previous tunnel ${oldTunnel.name} (continuing anyway)"
                        }
                    }
            }

            tunnelRepository.save(newTunnel.copy(active = true))

            return@withLock safeDaemonCall {
                    val request =
                        StartTunnelRequest(
                            name = newTunnel.name,
                            quickConfig = newTunnel.quickConfig,
                        )

                    client.post(Routes.Tunnels.start(id)) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                    Unit
                }
                .onFailure { error ->
                    tunnelRepository.save(newTunnel.copy(active = false))
                    Logger.e(error) { "Failed to start tunnel $id — rolled back DB state" }
                }
        }

    override suspend fun stopTunnel(id: Long): Result<Unit> =
        tunnelCommandMutex.withLock {
            val tunnelConfig =
                tunnelRepository.getById(id)
                    ?: return@withLock Result.failure(
                        ClientException.BadRequestException("Could not find tunnel with id=$id")
                    )

            tunnelRepository.save(tunnelConfig.copy(active = false))

            return@withLock safeDaemonCall {
                client.post(Routes.Tunnels.stop(id))
                Unit
            }
        }
}

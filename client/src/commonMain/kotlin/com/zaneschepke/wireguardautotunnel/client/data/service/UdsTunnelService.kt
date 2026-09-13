package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.service.TunnelService
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UdsTunnelService(private val client: HttpClient) : TunnelService {

    private val tunnelCommandMutex = Mutex()

    override suspend fun startTunnel(id: Long, request: StartTunnelRequest): Result<Unit> =
        tunnelCommandMutex.withLock {
            safeDaemonCall {
                client.post(Routes.Tunnels.start(id)) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                Unit
            }
        }

    override suspend fun stopTunnel(id: Long): Result<Unit> = tunnelCommandMutex.withLock {
        safeDaemonCall {
            client.post(Routes.Tunnels.stop(id))
            Unit
        }
    }
}

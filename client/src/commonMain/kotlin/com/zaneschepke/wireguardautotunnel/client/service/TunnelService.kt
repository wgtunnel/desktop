package com.zaneschepke.wireguardautotunnel.client.service

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest

interface TunnelService {
    suspend fun startTunnel(id: Long, request: StartTunnelRequest): Result<Unit>

    suspend fun stopTunnel(id: Long): Result<Unit>
}

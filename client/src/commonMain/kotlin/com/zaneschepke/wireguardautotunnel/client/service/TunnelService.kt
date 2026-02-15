package com.zaneschepke.wireguardautotunnel.client.service

interface TunnelService {
    suspend fun startTunnel(id: Long): Result<Unit>

    suspend fun stopTunnel(id: Long): Result<Unit>
}

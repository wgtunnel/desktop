package com.zaneschepke.wireguardautotunnel.client.service

interface TunnelCommandService {
    suspend fun startTunnel(id: Long): Result<Unit>

    suspend fun stopTunnel(id: Long): Result<Unit>
}

package com.zaneschepke.wireguardautotunnel.client.service

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import kotlinx.coroutines.flow.Flow

interface TunnelCommandService {
    suspend fun startTunnel(id: Long): Result<Unit>
    suspend fun stopTunnel(id: Long): Result<Unit>
}
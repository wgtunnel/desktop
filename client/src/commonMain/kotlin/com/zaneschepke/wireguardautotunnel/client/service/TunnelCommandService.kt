package com.zaneschepke.wireguardautotunnel.client.service

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import kotlinx.coroutines.flow.Flow

interface TunnelCommandService {
    suspend fun startTunnel(id: Int): Result<Unit>
    suspend fun stopTunnel(id: Int): Result<Unit>
    suspend fun setMode(mode: BackendMode): Result<Unit>
    suspend fun setKillSwitch(enabled: Boolean): Result<Unit>
    suspend fun getStatus(): Result<BackendStatus>
    fun statusFlow(): Flow<BackendStatus>
}
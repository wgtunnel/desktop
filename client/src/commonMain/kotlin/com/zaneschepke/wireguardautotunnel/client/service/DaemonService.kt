package com.zaneschepke.wireguardautotunnel.client.service

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import kotlinx.coroutines.flow.Flow

interface DaemonService {
    suspend fun alive(): Boolean

    suspend fun setRestoreKillSwitch(enabled: Boolean): Result<Unit>

    suspend fun setRestoreTunnel(enabled: Boolean): Result<Unit>

    suspend fun updateAutoTunnelConfig(plan: AutoTunnelConfigDto): Result<Unit>

    suspend fun getAutoTunnelStatus(): Result<AutoTunnelStatusDto>

    fun autoTunnelStatusFlow(): Flow<AutoTunnelStatusDto>

    suspend fun setLocalLogging(enabled: Boolean): Result<Unit>

    suspend fun clearLogs(): Result<Unit>

    suspend fun downloadLogZip(): Result<ByteArray>

    fun logsFlow(): Flow<LogMessageDto>

    val alive: Flow<Boolean>

    // Version reported by the currently connected daemon, null while disconnected.
    val remoteVersion: Flow<String?>
}

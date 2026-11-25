package com.zaneschepke.wireguardautotunnel.client.service

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.DaemonInfoDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import kotlinx.coroutines.flow.Flow

interface DaemonService {
    suspend fun alive(): Boolean

    suspend fun setRestoreKillSwitch(enabled: Boolean): Result<Unit>

    suspend fun updateAutoTunnelConfig(plan: AutoTunnelConfigDto): Result<Unit>

    suspend fun getAutoTunnelStatus(): Result<AutoTunnelStatusDto>

    fun autoTunnelStatusFlow(): Flow<AutoTunnelStatusDto>

    suspend fun setLocalLogging(enabled: Boolean): Result<Unit>

    suspend fun clearLogs(): Result<Unit>

    suspend fun downloadLogZip(): Result<ByteArray>

    fun logsFlow(): Flow<LogMessageDto>

    /** Asks a supervised daemon to exit so its service manager starts the installed binary. */
    suspend fun requestRestart(): Result<Unit>

    /**
     * Stops tunnels and removes the kill switch, its rules and the saved session, before the daemon
     * is unregistered.
     */
    suspend fun reset(): Result<Unit>

    val alive: Flow<Boolean>

    // Reported by the currently connected daemon, null while disconnected.
    val remoteInfo: Flow<DaemonInfoDto?>
}

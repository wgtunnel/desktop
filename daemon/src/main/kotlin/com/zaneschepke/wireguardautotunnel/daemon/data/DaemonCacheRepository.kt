package com.zaneschepke.wireguardautotunnel.daemon.data

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest

interface DaemonCacheRepository {
    suspend fun updateKillSwitchEnabled(enabled: Boolean)

    suspend fun updateKillSwitchConfig(config: KillSwitchConfigDto?)

    suspend fun updateKillSwitchRestore(enabled: Boolean)

    suspend fun getKillSwitchEnabled(): Boolean

    suspend fun getKillSwitchConfig(): KillSwitchConfigDto?

    suspend fun getKillSwitchRestore(): Boolean

    suspend fun updateLastStartRequest(tunnelId: Long, request: StartTunnelRequest)

    suspend fun getLastStartRequest(): Pair<Long, StartTunnelRequest>?

    suspend fun setRestoreTunnelOnBoot(enabled: Boolean)

    suspend fun getRestoreTunnelOnBoot(): Boolean

    suspend fun updateAutoTunnelPlan(plan: AutoTunnelConfigDto?)

    suspend fun getAutoTunnelPlan(): AutoTunnelConfigDto?

    suspend fun setLocalLoggingEnabled(enabled: Boolean)

    suspend fun getLocalLoggingEnabled(): Boolean
}

package com.zaneschepke.wireguardautotunnel.daemon.data

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto

interface DaemonCacheRepository {
    suspend fun updateKillSwitchEnabled(enabled: Boolean)

    suspend fun updateKillSwitchConfig(config: KillSwitchConfigDto?)

    suspend fun updateKillSwitchRestore(enabled: Boolean)

    suspend fun getKillSwitchEnabled(): Boolean

    suspend fun getKillSwitchConfig(): KillSwitchConfigDto?

    suspend fun getKillSwitchRestore(): Boolean

    suspend fun setLocalLoggingEnabled(enabled: Boolean)

    suspend fun getLocalLoggingEnabled(): Boolean
}

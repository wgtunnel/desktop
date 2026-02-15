package com.zaneschepke.wireguardautotunnel.daemon.data

interface DaemonCacheRepository {
    suspend fun updateKillSwitchEnabled(enabled: Boolean)

    suspend fun updateKillSwitchBypassLan(enabled: Boolean)

    suspend fun updateKillSwitchRestore(enabled: Boolean)

    suspend fun getKillSwitchEnabled(): Boolean

    suspend fun getKillSwitchBypassLan(): Boolean

    suspend fun getKillSwitchRestore(): Boolean

    suspend fun updateLastActiveTunnelConfig(quick: String)

    suspend fun getLastActiveTunnelConfig(): String?

    suspend fun updateLastActiveTunnelId(tunnelId: Long)

    suspend fun getLastActiveTunnelId(): Long?

    suspend fun updateLastActiveTunnelName(tunnelName: String)

    suspend fun getLastActiveTunnelName(): String?

    suspend fun setRestoreTunnelOnBoot(enabled: Boolean)

    suspend fun getRestoreTunnelOnBoot(): Boolean
}

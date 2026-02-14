package com.zaneschepke.wireguardautotunnel.daemon.data

import com.zaneschepke.wireguardautotunnel.daemon.data.model.KillSwitchSettings

interface DaemonCacheRepository {
    suspend fun getKillSwitchSettings(): KillSwitchSettings

    suspend fun setKillSwitchSettings(settings: KillSwitchSettings)

    suspend fun getStartConfigs(): Set<String>

    suspend fun setStartConfigs(configs: Set<String>)
}

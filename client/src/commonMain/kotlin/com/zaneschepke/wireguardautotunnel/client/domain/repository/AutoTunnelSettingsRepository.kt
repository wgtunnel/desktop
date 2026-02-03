package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.AutoTunnelSettings
import kotlinx.coroutines.flow.Flow

interface AutoTunnelSettingsRepository {
    suspend fun upsert(autoTunnelSettings: AutoTunnelSettings)

    val flow: Flow<AutoTunnelSettings>

    suspend fun getAutoTunnelSettings(): AutoTunnelSettings

    suspend fun updateAutoTunnelEnabled(enabled: Boolean)
}

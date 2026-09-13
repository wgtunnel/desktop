package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings
import kotlinx.coroutines.flow.Flow

interface MonitoringSettingsRepository {
    val flow: Flow<MonitoringSettings>

    suspend fun get(): MonitoringSettings

    suspend fun upsert(settings: MonitoringSettings)
}

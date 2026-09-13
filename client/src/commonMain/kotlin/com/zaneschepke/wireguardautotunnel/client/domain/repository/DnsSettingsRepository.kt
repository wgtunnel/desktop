package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings
import kotlinx.coroutines.flow.Flow

interface DnsSettingsRepository {
    val flow: Flow<DnsSettings>

    suspend fun get(): DnsSettings

    suspend fun upsert(settings: DnsSettings)
}

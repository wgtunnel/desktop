package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings
import kotlinx.coroutines.flow.Flow

interface DnsSettingsRepository {
    suspend fun upsert(dnsSettings: DnsSettings)

    val flow: Flow<DnsSettings>

    suspend fun getDnsSettings(): DnsSettings
}

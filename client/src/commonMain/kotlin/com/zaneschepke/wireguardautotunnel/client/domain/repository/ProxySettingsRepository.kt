package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings
import kotlinx.coroutines.flow.Flow

interface ProxySettingsRepository {
    suspend fun upsert(proxySettings: ProxySettings)

    val flow: Flow<ProxySettings>

    suspend fun getProxySettings(): ProxySettings
}

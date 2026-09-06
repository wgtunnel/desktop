package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings
import kotlinx.coroutines.flow.Flow

interface ProxySettingsRepository {
    val flow: Flow<ProxySettings>

    suspend fun get(): ProxySettings

    suspend fun upsert(settings: ProxySettings)
}

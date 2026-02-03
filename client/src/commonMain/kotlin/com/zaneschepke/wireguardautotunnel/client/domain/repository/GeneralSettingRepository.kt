package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings
import kotlinx.coroutines.flow.Flow

interface GeneralSettingRepository {
    suspend fun upsert(generalSettings: GeneralSettings)

    val flow: Flow<GeneralSettings>

    suspend fun getGeneralSettings(): GeneralSettings

    suspend fun updateTheme(theme: Theme)

    suspend fun updateLocale(locale: String)

    suspend fun updateAppMode(appMode: AppMode)
}
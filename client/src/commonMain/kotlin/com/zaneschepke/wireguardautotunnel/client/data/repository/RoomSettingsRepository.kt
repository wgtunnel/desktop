package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings as Domain
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import kotlinx.coroutines.flow.map

class RoomSettingsRepository(private val settingsDao: GeneralSettingsDao) :
    GeneralSettingRepository {

    override suspend fun upsert(generalSettings: Domain) {
        settingsDao.upsert(generalSettings.toEntity())
    }

    override val flow = settingsDao.getGeneralSettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun getGeneralSettings(): Domain {
        return (settingsDao.getGeneralSettings() ?: Entity()).toDomain()
    }

    override suspend fun updateTheme(theme: Theme) {
        settingsDao.updateTheme(theme.name)
    }

    override suspend fun updateLocale(locale: String) {
        settingsDao.updateLocale(locale)
    }

    override suspend fun updateAppMode(appMode: AppMode) {
        settingsDao.updateAppMode(appMode)
    }
}

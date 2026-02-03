package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.AutoTunnelSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.zaneschepke.wireguardautotunnel.client.data.entity.AutoTunnelSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.AutoTunnelSettings as Domain

class RoomAutoTunnelSettingsRepository(private val autoTunnelSettingsDao: AutoTunnelSettingsDao) :
    AutoTunnelSettingsRepository {
    override suspend fun upsert(autoTunnelSettings: Domain) {
        autoTunnelSettingsDao.upsert(autoTunnelSettings.toEntity())
    }

    override val flow: Flow<Domain>
        get() =
            autoTunnelSettingsDao.getAutoTunnelSettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun getAutoTunnelSettings(): Domain {
        return (autoTunnelSettingsDao.getAutoTunnelSettings() ?: Entity()).toDomain()
    }

    override suspend fun updateAutoTunnelEnabled(enabled: Boolean) {
        autoTunnelSettingsDao.updateAutoTunnelEnabled(enabled)
    }
}

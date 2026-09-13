package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.MonitoringSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.MonitoringSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import kotlinx.coroutines.flow.map

class RoomMonitoringSettingsRepository(private val dao: MonitoringSettingsDao) :
    MonitoringSettingsRepository {
    override val flow = dao.getFlow().map { (it ?: Entity(id = 1)).toDomain() }

    override suspend fun get(): Domain = (dao.get() ?: Entity(id = 1)).toDomain()

    override suspend fun upsert(settings: Domain) {
        dao.upsert(settings.toEntity())
    }
}

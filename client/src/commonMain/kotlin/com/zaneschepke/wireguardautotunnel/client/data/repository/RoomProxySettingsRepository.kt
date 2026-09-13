package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import kotlinx.coroutines.flow.map

class RoomProxySettingsRepository(private val dao: ProxySettingsDao) : ProxySettingsRepository {
    override val flow = dao.getFlow().map { (it ?: Entity(id = 1)).toDomain() }

    override suspend fun get(): Domain = (dao.get() ?: Entity(id = 1)).toDomain()

    override suspend fun upsert(settings: Domain) {
        dao.upsert(settings.toEntity())
    }
}

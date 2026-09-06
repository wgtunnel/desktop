package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import kotlinx.coroutines.flow.map

class RoomDnsSettingsRepository(private val dao: DnsSettingsDao) : DnsSettingsRepository {
    override val flow = dao.getFlow().map { (it ?: Entity(id = 1)).toDomain() }

    override suspend fun get(): Domain = (dao.get() ?: Entity(id = 1)).toDomain()

    override suspend fun upsert(settings: Domain) {
        dao.upsert(settings.toEntity())
    }
}

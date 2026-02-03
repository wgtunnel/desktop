package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings as Domain

class RoomDnsSettingsRepository(private val dnsSettingsDao: DnsSettingsDao) :
    DnsSettingsRepository {
    override suspend fun upsert(dnsSettings: Domain) {
        dnsSettingsDao.upsert(dnsSettings.toEntity())
    }

    override val flow: Flow<Domain>
        get() = dnsSettingsDao.getDnsSettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun getDnsSettings(): Domain {
        return (dnsSettingsDao.getDnsSettings() ?: Entity()).toDomain()
    }
}

package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import kotlinx.coroutines.flow.map
import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings as Domain

class RoomProxySettingsRepository(private val proxySettingsDao: ProxySettingsDao) :
    ProxySettingsRepository {

    override suspend fun upsert(proxySettings: Domain) {
        proxySettingsDao.upsert(proxySettings.toEntity())
    }

    override val flow = proxySettingsDao.getProxySettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun getProxySettings(): Domain {
        return (proxySettingsDao.getProxySettings() ?: Entity()).toDomain()
    }
}

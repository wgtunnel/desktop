package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import kotlinx.coroutines.flow.map
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings as Domain

class RoomLockdownSettingsRepository(private val lockdownSettingsDao: LockdownSettingsDao) :
    LockdownSettingsRepository {
    override suspend fun upsert(lockdownSettings: Domain) {
        lockdownSettingsDao.upsert(lockdownSettings.toEntity())
    }

    override val flow =
        lockdownSettingsDao.getLockdownSettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun getLockdownSettings(): Domain {
        return (lockdownSettingsDao.getLockdownSettings() ?: Entity()).toDomain()
    }
}

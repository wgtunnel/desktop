package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import kotlinx.coroutines.flow.map

class RoomLockdownSettingsRepository(private val lockdownSettingsDao: LockdownSettingsDao) :
    LockdownSettingsRepository {
    override suspend fun upsert(lockdownSettings: Domain) {
        lockdownSettingsDao.upsert(lockdownSettings.toEntity())
    }

    override suspend fun updateEnabled(enabled: Boolean) {
        lockdownSettingsDao.updateEnabled(enabled)
    }

    override suspend fun updateBypassLan(enabled: Boolean) {
        lockdownSettingsDao.updateBypassLan(enabled)
    }

    override suspend fun updateRestoreOnBoot(enabled: Boolean) {
        lockdownSettingsDao.updateRestoreEnabled(enabled)
    }

    override val flow =
        lockdownSettingsDao.getLockdownSettingsFlow().map { (it ?: Entity()).toDomain() }

    override suspend fun get(): Domain {
        return (lockdownSettingsDao.getLockdownSettings() ?: Entity()).toDomain()
    }
}

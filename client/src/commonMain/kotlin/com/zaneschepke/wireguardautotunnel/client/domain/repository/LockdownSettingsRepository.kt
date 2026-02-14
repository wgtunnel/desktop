package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings
import kotlinx.coroutines.flow.Flow

interface LockdownSettingsRepository {
    suspend fun upsert(lockdownSettings: LockdownSettings)

    suspend fun updateEnabled(enabled: Boolean)

    suspend fun updateBypassLan(enabled: Boolean)

    suspend fun updateRestoreOnBoot(enabled: Boolean)

    val flow: Flow<LockdownSettings>

    suspend fun get(): LockdownSettings
}

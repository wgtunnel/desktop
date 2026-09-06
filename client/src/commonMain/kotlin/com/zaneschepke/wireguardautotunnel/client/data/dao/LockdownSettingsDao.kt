package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LockdownSettingsDao {
    @Query("SELECT * FROM lockdown_settings LIMIT 1")
    suspend fun getLockdownSettings(): LockdownSettings?

    @Upsert suspend fun upsert(lockdownSettings: LockdownSettings)

    @Query("UPDATE lockdown_settings SET lockdown_enabled = :enabled WHERE id = 1")
    suspend fun updateEnabled(enabled: Boolean)

    @Query("UPDATE lockdown_settings SET restore_on_boot = :enabled WHERE id = 1")
    suspend fun updateRestoreEnabled(enabled: Boolean)

    @Query("UPDATE lockdown_settings SET bypass_lan = :enabled WHERE id = 1")
    suspend fun updateBypassLan(enabled: Boolean)

    @Query("SELECT * FROM lockdown_settings LIMIT 1")
    fun getLockdownSettingsFlow(): Flow<LockdownSettings?>
}

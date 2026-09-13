package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.MonitoringSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoringSettingsDao {
    @Query("SELECT * FROM monitoring_settings WHERE id = 1") suspend fun get(): MonitoringSettings?

    @Upsert suspend fun upsert(settings: MonitoringSettings)

    @Query("SELECT * FROM monitoring_settings WHERE id = 1")
    fun getFlow(): Flow<MonitoringSettings?>
}

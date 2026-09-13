package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySettingsDao {
    @Query("SELECT * FROM proxy_settings WHERE id = 1") suspend fun get(): ProxySettings?

    @Upsert suspend fun upsert(settings: ProxySettings)

    @Query("SELECT * FROM proxy_settings WHERE id = 1") fun getFlow(): Flow<ProxySettings?>
}

package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsSettingsDao {
    @Query("SELECT * FROM dns_settings WHERE id = 1") suspend fun get(): DnsSettings?

    @Upsert suspend fun upsert(settings: DnsSettings)

    @Query("SELECT * FROM dns_settings WHERE id = 1") fun getFlow(): Flow<DnsSettings?>
}

package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.AutoTunnelSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoTunnelSettingsDao {
    @Query("SELECT * FROM auto_tunnel_settings LIMIT 1")
    suspend fun getAutoTunnelSettings(): AutoTunnelSettings?

    @Upsert suspend fun upsert(autoTunnelSettings: AutoTunnelSettings)

    @Query("SELECT * FROM auto_tunnel_settings LIMIT 1")
    fun getAutoTunnelSettingsFlow(): Flow<AutoTunnelSettings?>

    @Query("UPDATE auto_tunnel_settings SET is_tunnel_enabled = :enabled")
    suspend fun updateAutoTunnelEnabled(enabled: Boolean)
}

package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneralSettingsDao {
    @Query("SELECT * FROM general_settings WHERE id = 1") suspend fun get(): GeneralSettings?

    @Upsert suspend fun upsert(generalSettings: GeneralSettings)

    @Query("SELECT * FROM general_settings WHERE id = 1") fun getFlow(): Flow<GeneralSettings?>

    @Query("UPDATE general_settings SET theme = :theme WHERE id = 1")
    suspend fun updateTheme(theme: String)

    @Query("UPDATE general_settings SET locale = :locale WHERE id = 1")
    suspend fun updateLocale(locale: String)

    @Query("UPDATE general_settings SET restore_tunnel_on_boot = :enabled WHERE id = 1")
    suspend fun updateRestoreTunnelOnBoot(enabled: Boolean)

    @Query("UPDATE general_settings SET already_donated = :donated WHERE id = 1")
    suspend fun updateAlreadyDonated(donated: Boolean)

    @Query("UPDATE general_settings SET use_system_colors = :enabled WHERE id = 1")
    suspend fun updateSystemColors(enabled: Boolean)

    @Query("UPDATE general_settings SET tunnel_mode = :mode WHERE id = 1")
    suspend fun updateTunnelMode(mode: Int)

    @Query("UPDATE general_settings SET seamless_recovery = :enabled WHERE id = 1")
    suspend fun updateSeamlessRecovery(enabled: Boolean)

    @Query("UPDATE general_settings SET seamless_recovery_bounce_delay_sec = :seconds WHERE id = 1")
    suspend fun updateSeamlessRecoveryBounceDelay(seconds: Int)

    @Query("UPDATE general_settings SET global_amnezia_enabled = :enabled WHERE id = 1")
    suspend fun updateGlobalAmneziaEnabled(enabled: Boolean)
}

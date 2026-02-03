package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneralSettingsDao {
    @Query("SELECT * FROM general_settings LIMIT 1")
    suspend fun getGeneralSettings(): GeneralSettings?

    @Upsert suspend fun upsert(generalSettings: GeneralSettings)

    @Query("SELECT * FROM general_settings LIMIT 1")
    fun getGeneralSettingsFlow(): Flow<GeneralSettings?>

    @Query("UPDATE general_settings SET theme = :theme WHERE id = 1")
    suspend fun updateTheme(theme: String)

    @Query("UPDATE general_settings SET locale = :locale WHERE id = 1")
    suspend fun updateLocale(locale: String)

    @Query("UPDATE general_settings SET app_mode = :appMode WHERE id = 1")
    suspend fun updateAppMode(appMode: AppMode)
}

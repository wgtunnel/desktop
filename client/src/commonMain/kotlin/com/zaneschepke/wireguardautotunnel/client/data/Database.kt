package com.zaneschepke.wireguardautotunnel.client.data

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig

@Database(
    entities = [TunnelConfig::class, LockdownSettings::class, GeneralSettings::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppKeyringConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tunnelConfigDao(): TunnelConfigDao

    abstract fun generalSettingsDao(): GeneralSettingsDao

    abstract fun lockdownSettingsDao(): LockdownSettingsDao

    companion object {
        const val DB_SECRET_KEY = "db_secret"
        const val DB_KEYRING = "wg_tunnel"
        const val DB_FILE_NAME = "wg_tunnel.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

package com.zaneschepke.wireguardautotunnel.client.data

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DeleteColumn
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.converter.StringListConverter
import com.zaneschepke.wireguardautotunnel.client.data.dao.AutoTunnelSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.MonitoringSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.MonitoringSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig

@Database(
    entities =
        [
            TunnelConfig::class,
            LockdownSettings::class,
            GeneralSettings::class,
            DnsSettings::class,
            MonitoringSettings::class,
            ProxySettings::class,
            AutoTunnelSettings::class,
        ],
    version = 3,
    exportSchema = true,
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3, spec = AppDatabase.Migrate2To3::class),
        ],
)
@ColumnTypeConverters(AppKeyringConverter::class, StringListConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tunnelConfigDao(): TunnelConfigDao

    abstract fun generalSettingsDao(): GeneralSettingsDao

    abstract fun lockdownSettingsDao(): LockdownSettingsDao

    abstract fun dnsSettingsDao(): DnsSettingsDao

    abstract fun monitoringSettingsDao(): MonitoringSettingsDao

    abstract fun proxySettingsDao(): ProxySettingsDao

    abstract fun autoTunnelSettingsDao(): AutoTunnelSettingsDao

    companion object {
        const val DB_SECRET_KEY = "db_secret"
        const val DB_KEYRING = "wg_tunnel"
        const val DB_FILE_NAME = "wg_tunnel.db"
    }

    @DeleteColumn(tableName = "tunnel_config", columnName = "isIpv4Preferred")
    @DeleteColumn(tableName = "tunnel_config", columnName = "active")
    class Migrate2To3 : AutoMigrationSpec {
        override suspend fun onPostMigrate(connection: SQLiteConnection) {
            connection.execSQL("INSERT INTO dns_settings DEFAULT VALUES")
            connection.execSQL("INSERT INTO monitoring_settings DEFAULT VALUES")
            connection.execSQL("INSERT INTO proxy_settings DEFAULT VALUES")
            connection.execSQL("INSERT INTO auto_tunnel_settings DEFAULT VALUES")
        }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

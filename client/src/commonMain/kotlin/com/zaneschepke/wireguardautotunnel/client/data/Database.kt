package com.zaneschepke.wireguardautotunnel.client.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.zaneschepke.wireguardautotunnel.client.data.dao.AutoTunnelSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.entity.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.keyring.Keyring
import org.apache.commons.lang3.SystemUtils
import java.io.File

@Database(entities = [TunnelConfig::class, ProxySettings::class, LockdownSettings::class,
    GeneralSettings::class, DnsSettings::class, AutoTunnelSettings::class], version = 1, exportSchema = true)
@TypeConverters(DatabaseConverters::class, AppKeyringConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tunnelConfigDao(): TunnelConfigDao

    abstract fun proxySettingsDao(): ProxySettingsDao

    abstract fun generalSettingsDao(): GeneralSettingsDao

    abstract fun autoTunnelSettingsDao(): AutoTunnelSettingsDao

    abstract fun lockdownSettingsDao(): LockdownSettingsDao

    abstract fun dnsSettingsDao(): DnsSettingsDao

    companion object {
        const val DB_SECRET_KEY = "db_secret"
        const val DB_KEYRING = "wg_tunnel"
        const val DB_FILE_NAME = "wg_tunnel.db"
        const val APP_NAME = "WGTunnel" // macos convention

        fun getDatabaseDir() : File {
            val home = System.getProperty("user.home")
            return when {
                SystemUtils.IS_OS_WINDOWS -> {
                    val appData = System.getenv("APPDATA") ?: "${System.getProperty("user.home")}\\AppData\\Roaming"
                    File("$appData\\$APP_NAME")
                }
                SystemUtils.IS_OS_MAC -> {
                    File("$home/Library/Application Support/$APP_NAME")
                }
                else -> {
                    val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
                    File("$xdgDataHome/${APP_NAME.lowercase()}") // linux lowercase convention
                }
            }
        }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
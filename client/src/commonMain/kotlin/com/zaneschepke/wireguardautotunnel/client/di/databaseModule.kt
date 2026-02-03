package com.zaneschepke.wireguardautotunnel.client.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zaneschepke.wireguardautotunnel.client.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.client.data.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.DatabaseCallback
import com.zaneschepke.wireguardautotunnel.client.data.DatabaseConverters
import com.zaneschepke.wireguardautotunnel.client.data.dao.*
import com.zaneschepke.wireguardautotunnel.client.data.repository.*
import com.zaneschepke.wireguardautotunnel.client.domain.repository.*
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.keyring.Keyring
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import java.io.File
import javax.crypto.SecretKey

val databaseModule = module {
    single<RoomDatabase.Callback> { DatabaseCallback(lazy { get<AppDatabase>() }) }
    single<SecretKey> {
        val dbKey = AppDatabase.DB_SECRET_KEY
        val keyring = Keyring(AppDatabase.DB_KEYRING)
        val encodedSecret = keyring.get(dbKey) ?: run {
            val secret = Crypto.generateRandomBase64EncodedAesKey()
            keyring.put(dbKey, secret)
            secret
        }
        Crypto.decodeKey(encodedSecret)
    }
    single<AppDatabase> {
        val dbFileName = AppDatabase.DB_FILE_NAME
        val dbDir = AppDatabase.getDatabaseDir()
        dbDir.mkdirs()
        val dbFile = File(dbDir, dbFileName)
        Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .addCallback(get())
            .addTypeConverter(DatabaseConverters())
            .addTypeConverter(AppKeyringConverter())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single<TunnelConfigDao> { get<AppDatabase>().tunnelConfigDao() }
    single<AutoTunnelSettingsDao> { get<AppDatabase>().autoTunnelSettingsDao() }
    single<DnsSettingsDao> { get<AppDatabase>().dnsSettingsDao() }
    single<LockdownSettingsDao> { get<AppDatabase>().lockdownSettingsDao() }
    single<ProxySettingsDao> { get<AppDatabase>().proxySettingsDao() }
    single<GeneralSettingsDao> { get<AppDatabase>().generalSettingsDao() }

    single<TunnelRepository>() { RoomTunnelRepository(get()) }
    single<AutoTunnelSettingsRepository>() { RoomAutoTunnelSettingsRepository(get()) }
    single<DnsSettingsRepository>() { RoomDnsSettingsRepository(get()) }
    single<LockdownSettingsRepository>() { RoomLockdownSettingsRepository(get()) }
    single<ProxySettingsRepository>() { RoomProxySettingsRepository(get()) }
}
package com.zaneschepke.wireguardautotunnel.client.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zaneschepke.wireguardautotunnel.client.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.client.data.AppDatabaseConstructor
import com.zaneschepke.wireguardautotunnel.client.data.DatabaseCallback
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.dao.AutoTunnelSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.DnsSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.MonitoringSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.ProxySettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomAutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomDnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomLockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomMonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomTunnelRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.keyring.Keyring
import java.io.File
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val databaseModule = module {
    single<RoomDatabase.Callback> { DatabaseCallback(lazy { get<AppDatabase>() }) }
    single<SecretKey> {
        val dbKey = AppDatabase.DB_SECRET_KEY
        val keyring = Keyring(AppVariant.current.keyringService)
        val encodedSecret =
            keyring.get(dbKey)
                ?: run {
                    val secret = Crypto.generateRandomBase64EncodedAesKey()
                    keyring.put(dbKey, secret)
                    secret
                }
        Crypto.decodeKey(encodedSecret)
    }
    single<AppDatabase> {
        val dbFileName = AppDatabase.DB_FILE_NAME
        val dbDir = FilePathsHelper.getDatabaseDir()
        dbDir.mkdirs()
        val dbFile = File(dbDir, dbFileName)
        Room.databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = { AppDatabaseConstructor.initialize() },
            )
            .setDriver(BundledSQLiteDriver())
            .addCallback(get())
            .fallbackToDestructiveMigration(true)
            .addColumnTypeConverter(AppKeyringConverter())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single<TunnelConfigDao> { get<AppDatabase>().tunnelConfigDao() }
    single<LockdownSettingsDao> { get<AppDatabase>().lockdownSettingsDao() }
    single<GeneralSettingsDao> { get<AppDatabase>().generalSettingsDao() }
    single<DnsSettingsDao> { get<AppDatabase>().dnsSettingsDao() }
    single<MonitoringSettingsDao> { get<AppDatabase>().monitoringSettingsDao() }
    single<ProxySettingsDao> { get<AppDatabase>().proxySettingsDao() }
    single<AutoTunnelSettingsDao> { get<AppDatabase>().autoTunnelSettingsDao() }

    single<TunnelRepository> { RoomTunnelRepository(get()) }
    single<LockdownSettingsRepository> { RoomLockdownSettingsRepository(get()) }
    single<GeneralSettingRepository> { RoomSettingsRepository(get()) }
    single<DnsSettingsRepository> { RoomDnsSettingsRepository(get()) }
    single<MonitoringSettingsRepository> { RoomMonitoringSettingsRepository(get()) }
    single<ProxySettingsRepository> { RoomProxySettingsRepository(get()) }
    single<AutoTunnelSettingsRepository> { RoomAutoTunnelSettingsRepository(get()) }
}

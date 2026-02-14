package com.zaneschepke.wireguardautotunnel.client.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zaneschepke.wireguardautotunnel.client.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.client.data.DatabaseCallback
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.dao.GeneralSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.LockdownSettingsDao
import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomLockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.data.repository.RoomTunnelRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.keyring.Keyring
import java.io.File
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val databaseModule = module {
    single<RoomDatabase.Callback> { DatabaseCallback(lazy { get<AppDatabase>() }) }
    single<SecretKey> {
        val dbKey = AppDatabase.DB_SECRET_KEY
        val keyring = Keyring(AppDatabase.DB_KEYRING)
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
        val dbDir = AppDatabase.getDatabaseDir()
        dbDir.mkdirs()
        val dbFile = File(dbDir, dbFileName)
        Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .addCallback(get())
            .addTypeConverter(AppKeyringConverter())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single<TunnelConfigDao> { get<AppDatabase>().tunnelConfigDao() }
    single<LockdownSettingsDao> { get<AppDatabase>().lockdownSettingsDao() }
    single<GeneralSettingsDao> { get<AppDatabase>().generalSettingsDao() }

    single<TunnelRepository>() { RoomTunnelRepository(get()) }
    single<LockdownSettingsRepository>() { RoomLockdownSettingsRepository(get()) }
    single<GeneralSettingRepository>() { RoomSettingsRepository(get()) }
}

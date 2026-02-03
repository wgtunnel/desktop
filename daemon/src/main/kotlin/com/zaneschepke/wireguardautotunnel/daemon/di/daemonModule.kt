package com.zaneschepke.wireguardautotunnel.daemon.di

import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.daemon.TunnelDaemon
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.data.KStoreDaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.tunnel.AmneziaBackend
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val daemonModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<Backend> { AmneziaBackend() }
    single<DaemonCacheRepository> { KStoreDaemonCacheRepository() }
    single { TunnelDaemon(get(), get(), get(), IPC.getDaemonSocketPath()) }
}
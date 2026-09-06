package com.zaneschepke.wireguardautotunnel.desktop.di

import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AutoTunnelViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.DnsViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.GlobalConfigViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.LoggerViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.MonitoringViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.ProxyViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SupportViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    single { AppUpdater() }
    viewModelOf(::AppViewModel)
    viewModelOf(::TunnelsViewModel)
    viewModel { (id: Long) -> TunnelViewModel(get(), get(), id) }
    viewModelOf(::GlobalConfigViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::DnsViewModel)
    viewModelOf(::ProxyViewModel)
    viewModelOf(::MonitoringViewModel)
    viewModelOf(::SupportViewModel)
    viewModelOf(::AutoTunnelViewModel)
    viewModelOf(::LoggerViewModel)
}

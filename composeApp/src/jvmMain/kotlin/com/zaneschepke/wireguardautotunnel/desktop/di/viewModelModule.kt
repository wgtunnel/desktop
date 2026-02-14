package com.zaneschepke.wireguardautotunnel.desktop.di

import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::TunnelsViewModel)
    viewModel { (id: Long) -> TunnelViewModel(get(), get(), id) }
    viewModelOf(::SettingsViewModel)
}

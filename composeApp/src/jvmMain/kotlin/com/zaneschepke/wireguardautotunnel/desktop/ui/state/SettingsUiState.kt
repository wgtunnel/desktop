package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val settings: GeneralSettings = GeneralSettings(),
    val lockdown: LockdownSettings = LockdownSettings(),
    val monitoring: MonitoringSettings = MonitoringSettings(),
    val dns: DnsSettings = DnsSettings(),
    val globalTunnelConfig: TunnelConfig? = null,
    val updateBusy: Boolean = false,
    val updateMessage: String? = null,
    val pendingUpdateVersion: String? = null,
) {
    val tunnelMode: TunnelMode
        get() = settings.selectableTunnelMode

    val lockdownEnabled: Boolean
        get() = lockdown.enabled
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus

enum class DaemonConnectionStatus {
    DISCONNECTED,
    // Daemon socket is reachable, but we haven't received a real tunnel/kill-switch snapshot
    // from it yet
    SYNCING,
    CONNECTED,
}

data class AppUiState(
    val isLoaded: Boolean = false,
    val theme: Theme = Theme.DEFAULT,
    val useSystemColors: Boolean = false,
    val customSeedColor: Int? = null,
    val accentStyle: AccentStyle = AccentStyle.TONAL_SPOT,
    val daemonStatus: DaemonConnectionStatus = DaemonConnectionStatus.DISCONNECTED,
    val locale: String = DEFAULT_LOCALE,
    val alreadyDonated: Boolean = false,
    val restoreTunnelOnBoot: Boolean = false,
    val launchAtLogin: Boolean = true,
    val lockdownActive: Boolean = false,
    val autoTunnelEnabled: Boolean = false,
    val tunnelStatuses: List<TunnelStatus> = emptyList(),
    val tunnelMode: TunnelMode = TunnelMode.VPN,
) {
    companion object {
        const val DEFAULT_LOCALE = "en-US"
    }
}

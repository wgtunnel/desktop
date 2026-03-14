package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus

data class AppUiState(
    val isLoaded: Boolean = false,
    val theme: Theme = Theme.DARK,
    val useSystemColors: Boolean = false,
    val daemonConnected: Boolean = false,
    val locale: String = DEFAULT_LOCALE,
    val alreadyDonated: Boolean = false,
    val lockdownActive: Boolean = false,
    val tunnelStatuses: List<TunnelStatus> = emptyList(),
) {
    companion object {
        const val DEFAULT_LOCALE = "en-US"
    }
}

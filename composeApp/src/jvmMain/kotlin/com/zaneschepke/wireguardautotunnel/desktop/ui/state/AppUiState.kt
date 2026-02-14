package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.data.model.Theme

data class AppUiState(
    val isLoaded: Boolean = false,
    val theme: Theme = Theme.DARK,
    val daemonConnected: Boolean = false,
    val locale: String = DEFAULT_LOCALE,
    val alreadyDonated: Boolean = false,
    val lockdownActive: Boolean = false,
) {
    companion object {
        const val DEFAULT_LOCALE = "en-US"
    }
}

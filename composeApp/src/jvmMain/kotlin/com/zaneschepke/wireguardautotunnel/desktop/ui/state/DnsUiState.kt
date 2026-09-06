package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings

data class DnsUiState(
    val isLoaded: Boolean = false,
    val draft: DnsSettings = DnsSettings(),
    val saved: DnsSettings = DnsSettings(),
) {
    val isDirty: Boolean
        get() = draft != saved
}

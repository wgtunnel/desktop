package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings

data class ProxyUiState(
    val isLoaded: Boolean = false,
    val draft: ProxySettings = ProxySettings(),
    val saved: ProxySettings = ProxySettings(),
    val socksBindAddress: String = "",
    val httpBindAddress: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
) {
    val isDirty: Boolean
        get() = draft != saved
}

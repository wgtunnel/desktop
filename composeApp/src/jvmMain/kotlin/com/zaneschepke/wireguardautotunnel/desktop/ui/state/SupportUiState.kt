package com.zaneschepke.wireguardautotunnel.desktop.ui.state

data class SupportUiState(
    val updateBusy: Boolean = false,
    val pendingUpdateVersion: String? = null,
    val updateSupported: Boolean = false,
)

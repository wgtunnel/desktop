package com.zaneschepke.wireguardautotunnel.desktop.ui.state

data class SupportUiState(
    val updateBusy: Boolean = false,
    val updateMessage: String? = null,
    val pendingUpdateVersion: String? = null,
    val updateSupported: Boolean = true,
    val alreadyLatest: Boolean = false,
)

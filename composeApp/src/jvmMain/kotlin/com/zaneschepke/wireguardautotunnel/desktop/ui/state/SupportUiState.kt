package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.desktop.update.UpdateState

data class SupportUiState(
    val updateState: UpdateState = UpdateState.Idle,
    val updateSupported: Boolean = false,
)

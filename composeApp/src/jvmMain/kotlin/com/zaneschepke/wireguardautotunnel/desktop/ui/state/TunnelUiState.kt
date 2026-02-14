package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.parser.ActiveConfig

data class TunnelUiState(
    val isLoaded: Boolean = false,
    val originalConfig: TunnelConfig = TunnelConfig.Empty,
    val editedConfig: TunnelConfig = TunnelConfig.Empty,
    val tunnelState: TunnelState? = null,
    val activeConfig: ActiveConfig? = null,
    val isDirty: Boolean = false,
)

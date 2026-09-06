package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.wgtunnel.parser.ActiveConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig

data class TunnelUiState(
    val isLoaded: Boolean = false,
    val editedConfig: TunnelConfig = TunnelConfig.Empty,
    val currentConfig: TunnelConfig = TunnelConfig.Empty,
    val activeConfig: ActiveConfig? = null,
    val lastStatsAtMs: Long = 0L,
    val isDirty: Boolean = false,
)

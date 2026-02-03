package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendStatus(
    val killSwitchEnabled: Boolean,
    val mode: BackendMode,
    val activeTunnels: List<TunnelStatus>
)
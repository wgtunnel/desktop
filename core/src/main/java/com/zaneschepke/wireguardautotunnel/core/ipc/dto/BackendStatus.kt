package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendStatus(
    val killSwitchEnabled: Boolean = false,
    val mode: BackendMode = BackendMode.USERSPACE,
    val activeTunnels: List<TunnelStatus> = emptyList(),
)

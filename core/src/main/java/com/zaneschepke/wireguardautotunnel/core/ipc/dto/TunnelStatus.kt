package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class TunnelStatus(
    val id: Long,
    val name: String,
    val state: TunnelState
)
package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class TunnelStatus(
    val id: Int,
    val name: String,
    val state: TunnelState
)
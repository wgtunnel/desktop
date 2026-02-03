package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class StartTunnelRequest(
    val id: Int,
    val name: String,
    val quickConfig: String
)
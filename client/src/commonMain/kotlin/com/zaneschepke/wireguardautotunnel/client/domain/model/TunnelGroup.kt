package com.zaneschepke.wireguardautotunnel.client.domain.model

data class TunnelGroup(
    val id: Long = 0,
    val name: String,
    val position: Int = 0,
    val expanded: Boolean = true,
)

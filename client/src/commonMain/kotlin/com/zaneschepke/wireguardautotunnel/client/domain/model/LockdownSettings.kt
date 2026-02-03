package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LockdownSettings(
    val id: Long = 0L,
    val bypassLan: Boolean = false,
    val metered: Boolean = false,
    val dualStack: Boolean = false,
)

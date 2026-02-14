package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LockdownSettings(
    val id: Long = 1L,
    val enabled: Boolean = false,
    val restoreOnBoot: Boolean = false,
    val bypassLan: Boolean = false,
)

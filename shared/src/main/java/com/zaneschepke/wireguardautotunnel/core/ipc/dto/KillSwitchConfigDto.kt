package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class KillSwitchConfigDto(
    val allowedIps: Set<String> = emptySet(),
    val dualStack: Boolean = true,
)

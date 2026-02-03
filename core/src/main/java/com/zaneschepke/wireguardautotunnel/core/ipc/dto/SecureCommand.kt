package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class SecureCommand(
    val timestamp: Long,
    val signature: String,
    val userHint: String,
    val payload: String? = null
)
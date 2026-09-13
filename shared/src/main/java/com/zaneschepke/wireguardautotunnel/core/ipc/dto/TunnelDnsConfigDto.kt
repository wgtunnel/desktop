package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class TunnelDnsConfigDto(
    val defaultTransport: String,
    val localSuffixes: List<String> = emptyList(),
    val upstream: List<String> = emptyList(),
    val serverName: String? = null,
    val foreignDnsPolicy: String = "REDIRECT",
    val splitMode: String = "SYSTEM",
)

package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DnsSettings(
    val id: Long = 0,
    val dnsProtocol: Int = 0,
    val dnsEndpoint: String? = null,
    val isGlobalTunnelDnsEnabled: Boolean = false,
)

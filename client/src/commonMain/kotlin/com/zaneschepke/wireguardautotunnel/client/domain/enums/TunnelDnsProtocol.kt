package com.zaneschepke.wireguardautotunnel.client.domain.enums

import com.wgtunnel.backend.model.dns.DnsEndpointProtocol

enum class TunnelDnsProtocol(val value: Int) {
    Doh(0),
    Dot(1),
    Plain(2);

    fun toCore(): DnsEndpointProtocol =
        when (this) {
            Doh -> DnsEndpointProtocol.DOH
            Dot -> DnsEndpointProtocol.DOT
            Plain -> DnsEndpointProtocol.UDP
        }

    companion object {
        fun fromValue(value: Int): TunnelDnsProtocol = entries.find { it.value == value } ?: Doh
    }
}

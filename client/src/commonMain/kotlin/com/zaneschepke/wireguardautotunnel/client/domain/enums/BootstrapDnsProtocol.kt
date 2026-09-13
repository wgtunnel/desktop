package com.zaneschepke.wireguardautotunnel.client.domain.enums

import com.wgtunnel.backend.model.dns.DnsEndpointProtocol

enum class BootstrapDnsProtocol(val value: Int) {
    SYSTEM(0),
    DOH(1),
    DOT(2),
    UDP(3);

    fun toCore(): DnsEndpointProtocol =
        when (this) {
            SYSTEM -> DnsEndpointProtocol.SYSTEM
            DOH -> DnsEndpointProtocol.DOH
            DOT -> DnsEndpointProtocol.DOT
            UDP -> DnsEndpointProtocol.UDP
        }

    companion object {
        fun fromValue(value: Int): BootstrapDnsProtocol =
            entries.find { it.value == value } ?: SYSTEM
    }
}

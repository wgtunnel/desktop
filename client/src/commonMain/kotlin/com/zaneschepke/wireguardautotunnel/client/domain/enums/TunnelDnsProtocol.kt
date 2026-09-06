package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class TunnelDnsProtocol(val value: Int) {
    Doh(0),
    Dot(1),
    Plain(2);

    companion object {
        fun fromValue(value: Int): TunnelDnsProtocol = entries.find { it.value == value } ?: Doh
    }
}

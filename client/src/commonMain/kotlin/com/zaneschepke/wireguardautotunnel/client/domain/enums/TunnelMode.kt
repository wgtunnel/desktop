package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class TunnelMode(val value: Int) {
    VPN(0),
    PROXY(1);

    companion object {
        fun fromValue(value: Int): TunnelMode = entries.find { it.value == value } ?: VPN
    }
}

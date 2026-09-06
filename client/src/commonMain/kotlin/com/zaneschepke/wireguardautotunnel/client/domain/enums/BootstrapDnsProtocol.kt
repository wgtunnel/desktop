package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class BootstrapDnsProtocol(val value: Int) {
    SYSTEM(0),
    DOH(1),
    DOT(2),
    UDP(3);

    companion object {
        fun fromValue(value: Int): BootstrapDnsProtocol =
            entries.find { it.value == value } ?: SYSTEM
    }
}

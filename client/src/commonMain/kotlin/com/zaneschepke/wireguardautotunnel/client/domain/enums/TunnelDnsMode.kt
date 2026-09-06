package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class TunnelDnsMode(val value: Int) {
    Off(0),
    Encrypted(1),
    Split(2),
    AllLocal(3);

    fun isSplitMode(): Boolean = this == Split

    companion object {
        fun fromValue(value: Int): TunnelDnsMode =
            when (value) {
                4 -> Split // former SplitTunnel mode
                else -> entries.find { it.value == value } ?: Off
            }
    }
}

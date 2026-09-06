package com.zaneschepke.wireguardautotunnel.client.domain.enums

import com.wgtunnel.backend.model.dns.DnsSplitMode

enum class SplitDnsSuffixTarget(val value: Int) {
    System(0),
    Tunnel(1);

    fun toCore(): DnsSplitMode =
        when (this) {
            System -> DnsSplitMode.SYSTEM
            Tunnel -> DnsSplitMode.TUNNEL
        }

    companion object {
        fun fromValue(value: Int): SplitDnsSuffixTarget =
            entries.find { it.value == value } ?: System
    }
}

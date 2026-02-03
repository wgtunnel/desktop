package com.zaneschepke.wireguardautotunnel.client.data.model

enum class AppMode(val value: Int) {
    VPN(0),
    PROXY(1),
    LOCK_DOWN(2),
    KERNEL(3);

    companion object {
        fun fromValue(value: Int): com.zaneschepke.wireguardautotunnel.client.data.model.AppMode = entries.find { it.value == value } ?: VPN
    }
}

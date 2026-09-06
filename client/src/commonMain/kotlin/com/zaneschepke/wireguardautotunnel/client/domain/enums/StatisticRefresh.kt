package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class StatisticRefresh(val value: Int, val label: String) {
    LIVE(1, "Live (1s)"),
    BALANCED(3, "Balanced (3s)"),
    BATTERY_SAVER(10, "Slower (10s)");

    companion object {
        fun fromValue(value: Int): StatisticRefresh = entries.find { it.value == value } ?: BALANCED
    }
}

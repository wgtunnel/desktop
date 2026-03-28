package com.zaneschepke.wireguardautotunnel.desktop.util

enum class RenderingMode {
    HARDWARE,
    SOFTWARE;

    companion object {
        fun fromString(value: String?): RenderingMode =
            when (value?.trim()?.lowercase()) {
                "software" -> SOFTWARE
                else -> HARDWARE
            }
    }
}

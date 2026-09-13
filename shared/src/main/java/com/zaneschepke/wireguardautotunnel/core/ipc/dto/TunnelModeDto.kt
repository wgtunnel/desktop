package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TunnelModeDto {
    VPN,
    PROXY,
    LOCK_DOWN,
}

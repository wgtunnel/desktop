package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

/** Per-tunnel runtime mode as reported by the core backend. */
@Serializable
enum class BackendMode {
    VPN,
    PROXY,
    LOCK_DOWN,
    UNKNOWN,
}

package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TunnelState {
    DOWN,
    STARTING,
    HEALTHY,
    HANDSHAKE_FAILURE,
    RESOLVING_DNS,
    UNKNOWN,
}

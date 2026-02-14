package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request

import kotlinx.serialization.Serializable

@Serializable data class StartTunnelRequest(val name: String, val quickConfig: String)

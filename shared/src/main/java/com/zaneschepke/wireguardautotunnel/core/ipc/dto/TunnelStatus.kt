package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import com.wgtunnel.parser.ActiveConfig
import kotlinx.serialization.Serializable

@Serializable
data class TunnelStatus(
    val id: Long,
    val name: String,
    val state: TunnelState,
    val mode: BackendMode = BackendMode.UNKNOWN,
    val activeConfig: ActiveConfig? = null,
    val recoveryAttempts: Int = 0,
)

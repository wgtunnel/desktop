package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import kotlinx.serialization.Serializable

@Serializable
data class KillSwitchRequest(val enabled: Boolean, val config: KillSwitchConfigDto? = null)

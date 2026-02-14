package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request

import kotlinx.serialization.Serializable

@Serializable data class KillSwitchRequest(val enable: Boolean, val bypassLan: Boolean = false)

package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request
import kotlinx.serialization.Serializable
@Serializable
data class DirectWhitelistRequest(val entries: String = "")

package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request

import kotlinx.serialization.Serializable

@Serializable data class FlagRequest(val value: Boolean)

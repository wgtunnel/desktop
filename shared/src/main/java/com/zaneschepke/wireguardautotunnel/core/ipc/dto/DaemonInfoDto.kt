package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable data class DaemonInfoDto(val version: String)

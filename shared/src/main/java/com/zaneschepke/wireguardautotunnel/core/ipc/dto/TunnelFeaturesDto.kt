package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class TunnelFeaturesDto(
    val statisticsEnabled: Boolean = true,
    val statisticsPollIntervalSeconds: Int = 3,
    val seamlessRecoveryEnabled: Boolean = true,
    val dynamicDnsRecovery: Boolean = false,
)

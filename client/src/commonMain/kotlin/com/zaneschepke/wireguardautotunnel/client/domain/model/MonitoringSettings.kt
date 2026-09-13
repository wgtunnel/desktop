package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MonitoringSettings(
    val id: Long = 1L,
    val isLocalLogsEnabled: Boolean = false,
    val tunnelStatisticsEnabled: Boolean = true,
    val tunnelStatisticsPollInterval: Int = 3,
)

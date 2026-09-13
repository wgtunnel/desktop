package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.MonitoringSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        isLocalLogsEnabled = isLocalLogsEnabled,
        tunnelStatisticsEnabled = tunnelStatisticsEnabled,
        tunnelStatisticsPollInterval = tunnelStatisticsPollInterval,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        isLocalLogsEnabled = isLocalLogsEnabled,
        tunnelStatisticsEnabled = tunnelStatisticsEnabled,
        tunnelStatisticsPollInterval = tunnelStatisticsPollInterval,
    )

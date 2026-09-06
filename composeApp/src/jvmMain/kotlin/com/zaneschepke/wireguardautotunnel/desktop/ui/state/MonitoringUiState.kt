package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.enums.StatisticRefresh

data class MonitoringUiState(
    val isLoaded: Boolean = false,
    val tunnelStatisticsEnabled: Boolean = true,
    val statisticRefresh: StatisticRefresh = StatisticRefresh.BALANCED,
)

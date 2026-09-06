package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AutoTunnelSettings(
    val id: Long = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val trustedNetworkSsids: List<String> = emptyList(),
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isTunnelOnWifiEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val isStopOnNoInternetEnabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val trustedNetworkBssids: List<String> = emptyList(),
)

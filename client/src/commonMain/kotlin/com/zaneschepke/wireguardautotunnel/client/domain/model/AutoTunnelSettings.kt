package com.zaneschepke.wireguardautotunnel.client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AutoTunnelSettings(
    val id: Int = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val isTunnelOnMobileDataEnabled: Boolean = false,
     val trustedNetworkSSIDs: Set<String> = emptySet(),
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isTunnelOnWifiEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val isStopOnNoInternetEnabled: Boolean = false,
    val debounceDelaySeconds: Int = 3,
    val isTunnelOnUnsecureEnabled: Boolean = false,
    val wifiDetectionMethod: Int = 0,
    val startOnBoot: Boolean = false,
)

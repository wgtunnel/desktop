package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import kotlinx.serialization.Serializable

@Serializable
data class AutoTunnelConfigDto(
    val enabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val settings: AutoTunnelSettingsDto = AutoTunnelSettingsDto(),
    val tunnels: List<AutoTunnelTunnelConfigDto> = emptyList(),
)

@Serializable
data class AutoTunnelSettingsDto(
    val isTunnelOnWifiEnabled: Boolean = false,
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val isStopOnNoInternetEnabled: Boolean = false,
    val trustedNetworkSsids: List<String> = emptyList(),
    val trustedNetworkBssids: List<String> = emptyList(),
)

@Serializable
data class AutoTunnelTunnelConfigDto(
    val id: Long,
    val name: String,
    val isPrimaryTunnel: Boolean = false,
    val isEthernetTunnel: Boolean = false,
    val tunnelNetworks: List<String> = emptyList(),
    val tunnelBssids: List<String> = emptyList(),
    val startRequest: StartTunnelRequest,
)

@Serializable
data class NetworkStatusDto(
    val type: String = "disconnected",
    val ssid: String = "",
    val bssid: String = "",
)

@Serializable
data class AutoTunnelStatusDto(
    val running: Boolean = false,
    val enabled: Boolean = false,
    val network: NetworkStatusDto = NetworkStatusDto(),
)

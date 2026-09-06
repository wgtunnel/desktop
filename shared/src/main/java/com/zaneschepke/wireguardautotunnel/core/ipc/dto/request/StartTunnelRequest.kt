package com.zaneschepke.wireguardautotunnel.core.ipc.dto.request

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.ProxyConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelDnsConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelFeaturesDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelModeDto
import kotlinx.serialization.Serializable

@Serializable
data class StartTunnelRequest(
    val name: String,
    val quickConfig: String,
    val mode: TunnelModeDto = TunnelModeDto.VPN,
    val tunnelDns: TunnelDnsConfigDto? = null,
    val proxy: ProxyConfigDto? = null,
    val killSwitch: KillSwitchConfigDto? = null,
    val features: TunnelFeaturesDto = TunnelFeaturesDto(),
    val preferIpv6: Boolean = false,
    val ipv6RestoreEnabled: Boolean = false,
)

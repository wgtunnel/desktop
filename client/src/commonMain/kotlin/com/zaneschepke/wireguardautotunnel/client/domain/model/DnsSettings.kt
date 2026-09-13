package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import kotlinx.serialization.Serializable

@Serializable
data class DnsSettings(
    val id: Long = 1L,
    val bootstrapDnsProtocol: BootstrapDnsProtocol = BootstrapDnsProtocol.SYSTEM,
    val bootstrapDnsEndpoint: String? = null,
    val isGlobalTunnelConfigDnsEnabled: Boolean = false,
    val tunnelDnsMode: TunnelDnsMode = TunnelDnsMode.Off,
    val tunnelDnsProtocol: TunnelDnsProtocol = TunnelDnsProtocol.Doh,
    val tunnelDnsEndpoint: String? = null,
    val useTunnelDnsServersInSplit: Boolean = true,
    val localSuffixes: String? = null,
    val transitDnsPolicy: TransitDnsPolicy = TransitDnsPolicy.Redirect,
    val splitSuffixTarget: SplitDnsSuffixTarget = SplitDnsSuffixTarget.System,
)

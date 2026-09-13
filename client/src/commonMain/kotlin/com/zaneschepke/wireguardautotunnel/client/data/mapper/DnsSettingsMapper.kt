package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        bootstrapDnsProtocol = BootstrapDnsProtocol.fromValue(bootstrapDnsProtocol),
        bootstrapDnsEndpoint = bootstrapDnsEndpoint,
        isGlobalTunnelConfigDnsEnabled = isGlobalTunnelConfigDnsEnabled,
        tunnelDnsMode = TunnelDnsMode.fromValue(tunnelDnsMode),
        tunnelDnsProtocol = TunnelDnsProtocol.fromValue(tunnelDnsProtocol),
        tunnelDnsEndpoint = tunnelDnsEndpoint,
        useTunnelDnsServersInSplit = useTunnelDnsServersInSplit,
        localSuffixes = localSuffixes,
        transitDnsPolicy = TransitDnsPolicy.fromValue(transitDnsPolicy),
        splitSuffixTarget = SplitDnsSuffixTarget.fromValue(splitSuffixTarget),
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        bootstrapDnsProtocol = bootstrapDnsProtocol.value,
        bootstrapDnsEndpoint = bootstrapDnsEndpoint,
        isGlobalTunnelConfigDnsEnabled = isGlobalTunnelConfigDnsEnabled,
        tunnelDnsMode = tunnelDnsMode.value,
        tunnelDnsProtocol = tunnelDnsProtocol.value,
        tunnelDnsEndpoint = tunnelDnsEndpoint,
        useTunnelDnsServersInSplit = useTunnelDnsServersInSplit,
        localSuffixes = localSuffixes,
        transitDnsPolicy = transitDnsPolicy.value,
        splitSuffixTarget = splitSuffixTarget.value,
    )

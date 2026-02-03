package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol
import com.zaneschepke.wireguardautotunnel.client.data.entity.DnsSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        dnsProtocol = dnsProtocol.value,
        dnsEndpoint = dnsEndpoint,
        isGlobalTunnelDnsEnabled = isGlobalTunnelDnsEnabled,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        dnsProtocol = DnsProtocol.fromValue(dnsProtocol),
        dnsEndpoint = dnsEndpoint,
        isGlobalTunnelDnsEnabled = isGlobalTunnelDnsEnabled,
    )

package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig as Entity
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        name = name,
        quickConfig = quickConfig.value,
        isPrimaryTunnel = isPrimaryTunnel,
        position = position,
        preferIpv6 = preferIpv6,
        ipv6RestoreEnabled = ipv6RestoreEnabled,
        isDdnsTunnel = isDdnsTunnel,
        tunnelNetworks = tunnelNetworks,
        isEthernetTunnel = isEthernetTunnel,
        tunnelBssids = tunnelBssids,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        name = name,
        quickConfig = EncryptedField(quickConfig),
        isPrimaryTunnel = isPrimaryTunnel,
        position = position,
        preferIpv6 = preferIpv6,
        ipv6RestoreEnabled = ipv6RestoreEnabled,
        isDdnsTunnel = isDdnsTunnel,
        tunnelNetworks = tunnelNetworks,
        isEthernetTunnel = isEthernetTunnel,
        tunnelBssids = tunnelBssids,
    )

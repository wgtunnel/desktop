package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        name = name,
        quickConfig = quickConfig.value,
        tunnelNetworks = tunnelNetworks,
        isPrimaryTunnel = isPrimaryTunnel,
        active = active,
        pingTarget = pingTarget,
        isEthernetTunnel = isEthernetTunnel,
        isIpv4Preferred = isIpv4Preferred,
        position = position,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        name = name,
        quickConfig = EncryptedField(quickConfig),
        tunnelNetworks = tunnelNetworks,
        isPrimaryTunnel = isPrimaryTunnel,
        active = active,
        pingTarget = pingTarget,
        isEthernetTunnel = isEthernetTunnel,
        isIpv4Preferred = isIpv4Preferred,
        position = position,
    )

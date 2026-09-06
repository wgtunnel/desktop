package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.AutoTunnelSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.AutoTunnelSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        isAutoTunnelEnabled = isAutoTunnelEnabled,
        trustedNetworkSsids = trustedNetworkSsids,
        isTunnelOnEthernetEnabled = isTunnelOnEthernetEnabled,
        isTunnelOnWifiEnabled = isTunnelOnWifiEnabled,
        isWildcardsEnabled = isWildcardsEnabled,
        isStopOnNoInternetEnabled = isStopOnNoInternetEnabled,
        startOnBoot = startOnBoot,
        trustedNetworkBssids = trustedNetworkBssids,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        isAutoTunnelEnabled = isAutoTunnelEnabled,
        trustedNetworkSsids = trustedNetworkSsids,
        isTunnelOnEthernetEnabled = isTunnelOnEthernetEnabled,
        isTunnelOnWifiEnabled = isTunnelOnWifiEnabled,
        isWildcardsEnabled = isWildcardsEnabled,
        isStopOnNoInternetEnabled = isStopOnNoInternetEnabled,
        startOnBoot = startOnBoot,
        trustedNetworkBssids = trustedNetworkBssids,
    )

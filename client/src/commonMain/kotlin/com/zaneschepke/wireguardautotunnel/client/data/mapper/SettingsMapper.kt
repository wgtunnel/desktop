package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        theme = runCatching { Theme.valueOf(theme.uppercase()) }.getOrDefault(Theme.DARK),
        locale = locale,
        alreadyDonated = alreadyDonated,
        restoreTunnelOnBoot = restoreTunnelOnBoot,
        useSystemColors = useSystemColors,
        tunnelMode = TunnelMode.fromValue(tunnelMode),
        seamlessRecoveryEnabled = seamlessRecoveryEnabled,
        seamlessRecoveryBounceDelaySec = seamlessRecoveryBounceDelaySec,
        isGlobalAmneziaEnabled = isGlobalAmneziaEnabled,
        lastNotifiedUpdateVersion = lastNotifiedUpdateVersion,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        theme = theme.name,
        locale = locale,
        alreadyDonated = alreadyDonated,
        restoreTunnelOnBoot = restoreTunnelOnBoot,
        useSystemColors = useSystemColors,
        tunnelMode = tunnelMode.value,
        seamlessRecoveryEnabled = seamlessRecoveryEnabled,
        seamlessRecoveryBounceDelaySec = seamlessRecoveryBounceDelaySec,
        isGlobalAmneziaEnabled = isGlobalAmneziaEnabled,
        lastNotifiedUpdateVersion = lastNotifiedUpdateVersion,
    )

package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        theme = runCatching { Theme.valueOf(theme.uppercase()) }.getOrDefault(Theme.DEFAULT),
        locale = locale,
        alreadyDonated = alreadyDonated,
        restoreTunnelOnBoot = restoreTunnelOnBoot,
        launchAtLogin = launchAtLogin,
        useSystemColors = useSystemColors,
        customSeedColor = customSeedColor,
        accentStyle =
            runCatching { AccentStyle.valueOf(accentStyle.uppercase()) }
                .getOrDefault(AccentStyle.TONAL_SPOT),
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
        launchAtLogin = launchAtLogin,
        useSystemColors = useSystemColors,
        customSeedColor = customSeedColor,
        accentStyle = accentStyle.name,
        tunnelMode = tunnelMode.value,
        seamlessRecoveryEnabled = seamlessRecoveryEnabled,
        seamlessRecoveryBounceDelaySec = seamlessRecoveryBounceDelaySec,
        isGlobalAmneziaEnabled = isGlobalAmneziaEnabled,
        lastNotifiedUpdateVersion = lastNotifiedUpdateVersion,
    )

package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        theme = Theme.valueOf(theme.uppercase()),
        locale = locale,
        alreadyDonated = alreadyDonated,
        restoreTunnelOnBoot = restoreTunnelOnBoot,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        theme = theme.name,
        locale = locale,
        alreadyDonated = alreadyDonated,
        restoreTunnelOnBoot = restoreTunnelOnBoot,
    )

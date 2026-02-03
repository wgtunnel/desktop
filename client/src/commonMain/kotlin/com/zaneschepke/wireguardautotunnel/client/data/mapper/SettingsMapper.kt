package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.data.entity.GeneralSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        isRestoreOnBootEnabled = isRestoreOnBootEnabled,
        appMode = appMode,
        theme = Theme.valueOf(theme.uppercase()),
        locale = locale,
        alreadyDonated = alreadyDonated,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        isRestoreOnBootEnabled = isRestoreOnBootEnabled,
        appMode = appMode,
        theme = theme.name,
        locale = locale,
        alreadyDonated = alreadyDonated,
    )

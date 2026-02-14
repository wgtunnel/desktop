package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.LockdownSettings as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(id = id, enabled = enabled, restoreOnBoot = restoreOnBoot, bypassLan = bypassLan)

fun Domain.toEntity(): Entity =
    Entity(id = id, enabled = enabled, restoreOnBoot = restoreOnBoot, bypassLan = bypassLan)

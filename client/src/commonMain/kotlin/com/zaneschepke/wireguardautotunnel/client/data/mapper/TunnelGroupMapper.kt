package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelGroup as Entity
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup as Domain

fun Entity.toDomain(): Domain =
    Domain(id = id, name = name, position = position, expanded = expanded)

fun Domain.toEntity(): Entity =
    Entity(id = id, name = name, position = position, expanded = expanded)

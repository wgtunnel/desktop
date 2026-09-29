package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup
import kotlinx.coroutines.flow.Flow

interface TunnelGroupRepository {
    val flow: Flow<List<TunnelGroup>>

    suspend fun getAll(): List<TunnelGroup>

    suspend fun getById(id: Long): TunnelGroup?

    suspend fun save(group: TunnelGroup): Long

    suspend fun saveAll(groups: List<TunnelGroup>)

    suspend fun delete(id: Long)

    suspend fun setExpanded(id: Long, expanded: Boolean)
}

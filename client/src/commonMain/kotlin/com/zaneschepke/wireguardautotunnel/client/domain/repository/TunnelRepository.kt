package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import kotlinx.coroutines.flow.Flow

interface TunnelRepository {
    val flow: Flow<List<TunnelConfig>>

    val userTunnelsFlow: Flow<List<TunnelConfig>>

    val globalTunnelFlow: Flow<TunnelConfig?>

    suspend fun getAll(): List<TunnelConfig>

    suspend fun save(tunnel: TunnelConfig)

    suspend fun saveAll(tunnels: List<TunnelConfig>)

    suspend fun updateAll(tunnels: List<TunnelConfig>)

    suspend fun delete(id: Long)

    suspend fun deleteByName(name: String)

    suspend fun getById(id: Long): TunnelConfig?

    suspend fun getTunnelByName(name: String): TunnelConfig?

    suspend fun count(): Int

    suspend fun findByTunnelName(name: String): TunnelConfig?

    suspend fun delete(ids: List<Long>)

    suspend fun updatePrimaryTunnel(tunnel: TunnelConfig?)

    suspend fun updateEthernetTunnel(tunnel: TunnelConfig?)

    suspend fun setDdnsTunnel(id: Long, enabled: Boolean)

    suspend fun ensureGlobalConfigExists()
}

package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import kotlinx.coroutines.flow.Flow

interface TunnelRepository {
    val flow: Flow<List<TunnelConfig>>

    val userTunnelsFlow: Flow<List<TunnelConfig>>

    val globalTunnelFlow: Flow<TunnelConfig?>

    suspend fun getAll(): List<TunnelConfig>

    suspend fun save(tunnelConfig: TunnelConfig)

    suspend fun saveAll(tunnelConfigList: List<TunnelConfig>)

    suspend fun updatePrimaryTunnel(tunnelConfig: TunnelConfig?)

    suspend fun resetActiveTunnels()

    suspend fun updateEthernetTunnel(tunnelConfig: TunnelConfig?)

    suspend fun delete(tunnelConfig: TunnelConfig)

    suspend fun deleteByName(name: String)

    suspend fun getById(id: Int): TunnelConfig?

    suspend fun getActive(): List<TunnelConfig>

    suspend fun getDefaultTunnel(): TunnelConfig?

    suspend fun getStartTunnel(): TunnelConfig?

    suspend fun getTunnelByName(name: String): TunnelConfig?

    suspend fun count(): Int

    suspend fun findByTunnelName(name: String): TunnelConfig?

    suspend fun findByTunnelNetworksName(name: String): List<TunnelConfig>

    suspend fun findPrimary(): List<TunnelConfig>

    suspend fun delete(tunnels: List<TunnelConfig>)
}

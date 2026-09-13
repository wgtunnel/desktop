package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class RoomTunnelRepository(private val tunnelConfigDao: TunnelConfigDao) : TunnelRepository {

    override val flow =
        tunnelConfigDao.getAllFlow().map { it.map { tunnelConfig -> tunnelConfig.toDomain() } }

    override val userTunnelsFlow =
        tunnelConfigDao.getUserTunnelsFlow(Domain.GLOBAL_CONFIG_NAME).map { list ->
            list.map { it.toDomain() }
        }

    override val globalTunnelFlow =
        tunnelConfigDao.getGlobalTunnelFlow(Domain.GLOBAL_CONFIG_NAME).map { it?.toDomain() }

    override suspend fun getAll(): List<Domain> {
        return tunnelConfigDao.getAll().map { it.toDomain() }
    }

    override suspend fun save(tunnel: Domain) {
        tunnelConfigDao.upsert(tunnel.toEntity())
    }

    override suspend fun saveAll(tunnels: List<Domain>) {
        tunnelConfigDao.saveAll(tunnels.map { tunnelConfig -> tunnelConfig.toEntity() })
    }

    override suspend fun updateAll(tunnels: List<Domain>) {
        tunnelConfigDao.updateAll(tunnels.map { tunnelConfig -> tunnelConfig.toEntity() })
    }

    override suspend fun delete(id: Long) {
        tunnelConfigDao.deleteById(id)
    }

    override suspend fun deleteByName(name: String) {
        tunnelConfigDao.deleteByName(name)
    }

    override suspend fun getById(id: Long): Domain? {
        return tunnelConfigDao.getById(id)?.toDomain()
    }

    override suspend fun getTunnelByName(name: String): Domain? {
        return tunnelConfigDao.getByName(name)?.toDomain()
    }

    override suspend fun count(): Int {
        return tunnelConfigDao.count().toInt()
    }

    override suspend fun findByTunnelName(name: String): Domain? {
        return tunnelConfigDao.getByName(name)?.toDomain()
    }

    override suspend fun delete(ids: List<Long>) {
        tunnelConfigDao.deleteByIds(ids)
    }

    override suspend fun updatePrimaryTunnel(tunnel: Domain?) {
        tunnelConfigDao.resetPrimaryTunnel()
        tunnel?.let { save(it.copy(isPrimaryTunnel = true)) }
    }

    override suspend fun updateEthernetTunnel(tunnel: Domain?) {
        tunnelConfigDao.resetEthernetTunnel()
        tunnel?.let { save(it.copy(isEthernetTunnel = true)) }
    }

    override suspend fun setDdnsTunnel(id: Long, enabled: Boolean) {
        tunnelConfigDao.setDdnsTunnel(id, enabled)
    }

    override suspend fun ensureGlobalConfigExists() {
        if (globalTunnelFlow.firstOrNull() == null) {
            save(Domain.generateDefaultGlobalConfig())
        }
    }
}

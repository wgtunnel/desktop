package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.flow.map

class RoomTunnelRepository(private val tunnelConfigDao: TunnelConfigDao) : TunnelRepository {

    override val flow =
        tunnelConfigDao.getAllFlow().map { it.map { tunnelConfig -> tunnelConfig.toDomain() } }

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

    override suspend fun resetActiveTunnels() {
        tunnelConfigDao.resetActiveTunnels()
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

    override suspend fun getActive(): List<Domain> {
        return tunnelConfigDao.getActive().map { it.toDomain() }
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
}

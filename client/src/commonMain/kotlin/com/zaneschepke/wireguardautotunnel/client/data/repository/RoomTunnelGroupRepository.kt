package com.zaneschepke.wireguardautotunnel.client.data.repository

import com.zaneschepke.wireguardautotunnel.client.data.dao.TunnelGroupDao
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toDomain
import com.zaneschepke.wireguardautotunnel.client.data.mapper.toEntity
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup as Domain
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelGroupRepository
import kotlinx.coroutines.flow.map

class RoomTunnelGroupRepository(private val tunnelGroupDao: TunnelGroupDao) : TunnelGroupRepository {

    override val flow = tunnelGroupDao.getAllFlow().map { groups -> groups.map { it.toDomain() } }

    override suspend fun getAll(): List<Domain> {
        return tunnelGroupDao.getAll().map { it.toDomain() }
    }

    override suspend fun getById(id: Long): Domain? {
        return tunnelGroupDao.getById(id)?.toDomain()
    }

    override suspend fun save(group: Domain): Long {
        val rowId = tunnelGroupDao.upsert(group.toEntity())
        // Upsert returns -1 for an update, the id is already known then
        return if (rowId > 0) rowId else group.id
    }

    override suspend fun saveAll(groups: List<Domain>) {
        groups.forEach { tunnelGroupDao.upsert(it.toEntity()) }
    }

    override suspend fun delete(id: Long) {
        tunnelGroupDao.deleteById(id)
    }

    override suspend fun setExpanded(id: Long, expanded: Boolean) {
        tunnelGroupDao.setExpanded(id, expanded)
    }
}

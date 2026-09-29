package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelGroupDao {

    @Upsert suspend fun upsert(group: TunnelGroup): Long

    @Query("SELECT * FROM tunnel_group ORDER BY position ASC")
    fun getAllFlow(): Flow<List<TunnelGroup>>

    @Query("SELECT * FROM tunnel_group ORDER BY position ASC")
    suspend fun getAll(): List<TunnelGroup>

    @Query("SELECT * FROM tunnel_group WHERE id = :id") suspend fun getById(id: Long): TunnelGroup?

    @Query("DELETE FROM tunnel_group WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("UPDATE tunnel_group SET expanded = :expanded WHERE id = :id")
    suspend fun setExpanded(id: Long, expanded: Boolean)
}

package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {

    @Upsert suspend fun upsert(t: TunnelConfig)

    @Update suspend fun updateAll(tunnels: List<TunnelConfig>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: List<TunnelConfig>)

    @Query("SELECT * FROM tunnel_config WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("UPDATE tunnel_config SET active = 0 WHERE active = 1") suspend fun resetActiveTunnels()

    @Query("SELECT * FROM tunnel_config WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM tunnel_config WHERE active=1") suspend fun getActive(): List<TunnelConfig>

    @Query("SELECT * FROM tunnel_config") suspend fun getAll(): List<TunnelConfig>

    @Query("DELETE FROM tunnel_config WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("DELETE FROM tunnel_config WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM tunnel_config WHERE name = :name") suspend fun deleteByName(name: String)

    @Query("SELECT COUNT('id') FROM tunnel_config") suspend fun count(): Long

    @Query("SELECT * FROM tunnel_config ORDER BY position")
    fun getAllFlow(): Flow<List<TunnelConfig>>
}

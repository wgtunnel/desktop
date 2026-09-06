package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {

    @Upsert suspend fun upsert(t: TunnelConfig)

    @Update suspend fun updateAll(tunnels: List<TunnelConfig>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: List<TunnelConfig>)

    @Query("SELECT * FROM tunnel_config WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("SELECT * FROM tunnel_config WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM tunnel_config") suspend fun getAll(): List<TunnelConfig>

    @Query("DELETE FROM tunnel_config WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("DELETE FROM tunnel_config WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM tunnel_config WHERE name = :name") suspend fun deleteByName(name: String)

    @Query("SELECT COUNT('id') FROM tunnel_config") suspend fun count(): Long

    @Query("SELECT * FROM tunnel_config ORDER BY position")
    fun getAllFlow(): Flow<List<TunnelConfig>>

    @Query(
        "SELECT * FROM tunnel_config WHERE name != :globalName ORDER BY is_primary_tunnel DESC, position ASC"
    )
    fun getUserTunnelsFlow(globalName: String): Flow<List<TunnelConfig>>

    @Query("SELECT * FROM tunnel_config WHERE name = :globalName LIMIT 1")
    fun getGlobalTunnelFlow(globalName: String): Flow<TunnelConfig?>

    @Query("UPDATE tunnel_config SET is_ddns_tunnel = :value WHERE id = :id")
    suspend fun setDdnsTunnel(id: Long, value: Boolean)

    @Query("UPDATE tunnel_config SET is_primary_tunnel = 0 WHERE is_primary_tunnel = 1")
    suspend fun resetPrimaryTunnel()

    @Query("UPDATE tunnel_config SET is_ethernet_tunnel = 0 WHERE is_ethernet_tunnel = 1")
    suspend fun resetEthernetTunnel()

    @Query("SELECT * FROM tunnel_config WHERE is_primary_tunnel = 1")
    suspend fun findPrimary(): List<TunnelConfig>
}

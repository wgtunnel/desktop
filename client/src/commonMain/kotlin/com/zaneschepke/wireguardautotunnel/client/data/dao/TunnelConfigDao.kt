package com.zaneschepke.wireguardautotunnel.client.data.dao

import androidx.room.*
import com.zaneschepke.wireguardautotunnel.client.data.entity.TunnelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {

    @Upsert suspend fun upsert(t: TunnelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(t: List<TunnelConfig>)

    @Query("SELECT * FROM tunnel_config WHERE id=:id") suspend fun getById(id: Long): TunnelConfig?

    @Query("UPDATE tunnel_config SET active = 0 WHERE active = 1")
    suspend fun resetActiveTunnels()

    @Query("SELECT * FROM tunnel_config WHERE name=:name")
    suspend fun getByName(name: String): TunnelConfig?

    @Query("SELECT * FROM tunnel_config WHERE active=1")
    suspend fun getActive(): List<TunnelConfig>

    @Query("SELECT * FROM tunnel_config") suspend fun getAll(): List<TunnelConfig>

    @Delete suspend fun delete(t: TunnelConfig)

    @Delete suspend fun delete(t: List<TunnelConfig>)

    @Query("DELETE FROM tunnel_config WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT('id') FROM tunnel_config") suspend fun count(): Long

    @Query("SELECT * FROM tunnel_config WHERE tunnel_networks LIKE '%' || :name || '%'")
    suspend fun findByTunnelNetworkName(name: String): List<TunnelConfig>

    @Query("UPDATE tunnel_config SET is_primary_tunnel = 0 WHERE is_primary_tunnel =1")
    suspend fun resetPrimaryTunnel()

    @Query("UPDATE tunnel_config SET is_ethernet_tunnel = 0 WHERE is_ethernet_tunnel =1")
    suspend fun resetEthernetTunnel()

    @Query("SELECT * FROM tunnel_config WHERE is_primary_tunnel=1")
    suspend fun findByPrimary(): List<TunnelConfig>

    @Query(
        """
        SELECT * FROM tunnel_config
        WHERE name != '${TunnelConfig.GLOBAL_CONFIG_NAME}'
        ORDER BY
        CASE WHEN is_primary_tunnel = 1 THEN 0 ELSE 1 END,
        position ASC
        LIMIT 1
        """
    )
    suspend fun getDefaultTunnel(): TunnelConfig?

    @Query(
        """
        SELECT * FROM tunnel_config
        WHERE name != '${TunnelConfig.GLOBAL_CONFIG_NAME}'
        ORDER BY
        CASE WHEN active = 1 THEN 0
        WHEN is_primary_tunnel = 1 THEN 1
        ELSE 2 END,
        position ASC
        LIMIT 1
        """
    )
    suspend fun getStartTunnel(): TunnelConfig?

    @Query("SELECT * FROM tunnel_config ORDER BY position")
    fun getAllFlow(): Flow<List<TunnelConfig>>

    @Query("SELECT * FROM tunnel_config WHERE name != :globalName ORDER BY position")
    fun getAllTunnelsExceptGlobal(
        globalName: String = TunnelConfig.GLOBAL_CONFIG_NAME
    ): Flow<List<TunnelConfig>>

    @Query("SELECT * FROM tunnel_config WHERE name = :globalName LIMIT 1")
    fun getGlobalTunnel(globalName: String = TunnelConfig.GLOBAL_CONFIG_NAME): Flow<TunnelConfig?>
}

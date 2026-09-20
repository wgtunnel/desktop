package com.zaneschepke.wireguardautotunnel.client.domain.repository

/**
 * Lightweight, ephemeral client app cache deliberately not stored in the Room as it should not be
 * backed up. Tracks transient runtime facts, not saved preferences.
 */
interface ClientCacheRepository {
    suspend fun updateLastStartedTunnelId(id: Long)

    suspend fun getLastStartedTunnelId(): Long?
}

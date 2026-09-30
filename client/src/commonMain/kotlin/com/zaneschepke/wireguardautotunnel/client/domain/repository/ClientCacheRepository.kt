package com.zaneschepke.wireguardautotunnel.client.domain.repository

import com.zaneschepke.wireguardautotunnel.client.domain.model.WindowBounds

/**
 * Lightweight, ephemeral client app cache deliberately not stored in the Room as it should not be
 * backed up.
 */
interface ClientCacheRepository {
    suspend fun updateLastStartedTunnelId(id: Long)

    suspend fun getLastStartedTunnelId(): Long?

    suspend fun updateWindowBounds(bounds: WindowBounds)

    suspend fun getWindowBounds(): WindowBounds?
}

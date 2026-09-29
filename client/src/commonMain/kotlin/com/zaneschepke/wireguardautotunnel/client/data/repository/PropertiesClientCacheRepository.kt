package com.zaneschepke.wireguardautotunnel.client.data.repository

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.model.WindowBounds
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ClientCacheRepository
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

private val log = Logger.withTag("ClientCache")

class PropertiesClientCacheRepository(
    private val baseCacheDir: Path = FilePathsHelper.getClientCacheDir().toPath()
) : ClientCacheRepository {

    private val lock = Any()
    private val storePath = baseCacheDir.resolve(CACHE_FILE_NAME)
    private val props = Properties()

    init {
        if (Files.notExists(baseCacheDir)) {
            Files.createDirectories(baseCacheDir)
        }
        if (Files.exists(storePath) && Files.size(storePath) > 0) {
            Files.newInputStream(storePath).use { props.load(it) }
        }
    }

    override suspend fun updateLastStartedTunnelId(id: Long) {
        synchronized(lock) {
            props[KEY_LAST_STARTED_TUNNEL_ID] = id.toString()
            persistLocked()
        }
    }

    override suspend fun getLastStartedTunnelId(): Long? =
        synchronized(lock) { props.getProperty(KEY_LAST_STARTED_TUNNEL_ID)?.toLongOrNull() }

    override suspend fun updateWindowBounds(bounds: WindowBounds) {
        synchronized(lock) {
            props[KEY_WINDOW_WIDTH] = bounds.width.toString()
            props[KEY_WINDOW_HEIGHT] = bounds.height.toString()
            props[KEY_WINDOW_X] = bounds.x.toString()
            props[KEY_WINDOW_Y] = bounds.y.toString()
            props[KEY_WINDOW_MAXIMIZED] = bounds.isMaximized.toString()
            persistLocked()
        }
    }

    override suspend fun getWindowBounds(): WindowBounds? =
        synchronized(lock) {
            val width =
                props.getProperty(KEY_WINDOW_WIDTH)?.toFloatOrNull() ?: return@synchronized null
            val height =
                props.getProperty(KEY_WINDOW_HEIGHT)?.toFloatOrNull() ?: return@synchronized null
            val x = props.getProperty(KEY_WINDOW_X)?.toFloatOrNull() ?: return@synchronized null
            val y = props.getProperty(KEY_WINDOW_Y)?.toFloatOrNull() ?: return@synchronized null
            val isMaximized = props.getProperty(KEY_WINDOW_MAXIMIZED)?.toBoolean() == true
            WindowBounds(width, height, x, y, isMaximized)
        }

    private fun persistLocked() {
        try {
            FileOutputStream(storePath.toFile()).use { output ->
                props.store(output, "WG Tunnel Client Cache")
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to save client cache to disk" }
        }
    }

    companion object {
        const val CACHE_FILE_NAME = "cache.properties"

        private const val KEY_LAST_STARTED_TUNNEL_ID = "last_started_tunnel_id"
        private const val KEY_WINDOW_WIDTH = "window_width"
        private const val KEY_WINDOW_HEIGHT = "window_height"
        private const val KEY_WINDOW_X = "window_x"
        private const val KEY_WINDOW_Y = "window_y"
        private const val KEY_WINDOW_MAXIMIZED = "window_maximized"
    }
}

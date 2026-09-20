package com.zaneschepke.wireguardautotunnel.daemon.data

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlinx.serialization.json.Json

private val log = Logger.withTag("DaemonCache")

class SettingsDaemonCacheRepository(
    private val json: Json,
    private val baseCacheDir: Path = FilePathsHelper.getDaemonCacheBaseDir(),
) : DaemonCacheRepository {

    private val lock = Any()
    private val storePath = baseCacheDir.resolve(CACHE_FILE_NAME)
    private val props = Properties()

    init {
        if (Files.notExists(baseCacheDir)) {
            Files.createDirectories(baseCacheDir)
        }
        PermissionsHelper.secureDaemonDataDirectory(baseCacheDir)
        if (Files.exists(storePath) && Files.size(storePath) > 0) {
            Files.newInputStream(storePath).use { props.load(it) }
        }
        pruneObsoleteKeys()
    }

    // These all moved out of the daemon (they were per-user concepts with no way for the daemon to
    // know
    // which locally-authenticated user's data was even being restored on a multi-user machine)
    private fun pruneObsoleteKeys() {
        val obsoleteKeys =
            listOf(
                "last_start_request",
                "last_active_tunnel_id",
                "restore_on_boot",
                "auto_tunnel_plan",
            )
        synchronized(lock) {
            val removedAny = obsoleteKeys.map { props.remove(it) != null }.any { it }
            if (removedAny) persistLocked()
        }
    }

    override suspend fun updateKillSwitchEnabled(enabled: Boolean) =
        put(KEY_KS_ENABLED, enabled.toString())

    override suspend fun getKillSwitchEnabled(): Boolean = getBoolean(KEY_KS_ENABLED)

    override suspend fun updateKillSwitchConfig(config: KillSwitchConfigDto?) {
        if (config == null) {
            remove(KEY_KS_CONFIG)
        } else {
            put(KEY_KS_CONFIG, json.encodeToString(KillSwitchConfigDto.serializer(), config))
        }
    }

    override suspend fun getKillSwitchConfig(): KillSwitchConfigDto? {
        val raw = getString(KEY_KS_CONFIG) ?: return null
        return runCatching { json.decodeFromString(KillSwitchConfigDto.serializer(), raw) }
            .getOrNull()
    }

    override suspend fun updateKillSwitchRestore(enabled: Boolean) =
        put(KEY_KS_RESTORE, enabled.toString())

    override suspend fun getKillSwitchRestore(): Boolean = getBoolean(KEY_KS_RESTORE)

    override suspend fun setLocalLoggingEnabled(enabled: Boolean) =
        put(KEY_LOCAL_LOGGING, enabled.toString())

    override suspend fun getLocalLoggingEnabled(): Boolean = getBoolean(KEY_LOCAL_LOGGING)

    private fun getString(key: String): String? = synchronized(lock) { props.getProperty(key) }

    private fun getBoolean(key: String): Boolean = getString(key)?.toBoolean() == true

    private fun put(key: String, value: String) {
        synchronized(lock) {
            props[key] = value
            persistLocked()
        }
    }

    private fun remove(key: String) {
        synchronized(lock) {
            props.remove(key)
            persistLocked()
        }
    }

    private fun persistLocked() {
        try {
            FileOutputStream(storePath.toFile()).use { output ->
                props.store(output, "WireGuard AutoTunnel Daemon Cache")
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to save settings to disk" }
        }
    }

    companion object {
        const val CACHE_FILE_NAME = "cache.properties"

        private const val KEY_KS_ENABLED = "killswitch_enabled"
        private const val KEY_KS_CONFIG = "killswitch_config"
        private const val KEY_KS_RESTORE = "killswitch_restore"
        private const val KEY_LOCAL_LOGGING = "local_logging"
    }
}

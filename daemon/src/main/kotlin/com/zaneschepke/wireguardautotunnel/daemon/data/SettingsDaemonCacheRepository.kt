package com.zaneschepke.wireguardautotunnel.daemon.data

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
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

    override suspend fun updateLastStartRequest(tunnelId: Long, request: StartTunnelRequest) {
        synchronized(lock) {
            props[KEY_LAST_TUNNEL_ID] = tunnelId.toString()
            props[KEY_LAST_START] = json.encodeToString(StartTunnelRequest.serializer(), request)
            persistLocked()
        }
    }

    override suspend fun getLastStartRequest(): Pair<Long, StartTunnelRequest>? {
        val id = getLong(KEY_LAST_TUNNEL_ID) ?: return null
        val raw = getString(KEY_LAST_START) ?: return null
        val request =
            runCatching { json.decodeFromString(StartTunnelRequest.serializer(), raw) }.getOrNull()
                ?: return null
        return id to request
    }

    override suspend fun setRestoreTunnelOnBoot(enabled: Boolean) =
        put(KEY_RESTORE_ON_BOOT, enabled.toString())

    override suspend fun getRestoreTunnelOnBoot(): Boolean = getBoolean(KEY_RESTORE_ON_BOOT)

    override suspend fun updateAutoTunnelPlan(plan: AutoTunnelConfigDto?) {
        if (plan == null) {
            remove(KEY_AUTO_TUNNEL_PLAN)
        } else {
            put(KEY_AUTO_TUNNEL_PLAN, json.encodeToString(AutoTunnelConfigDto.serializer(), plan))
        }
    }

    override suspend fun getAutoTunnelPlan(): AutoTunnelConfigDto? {
        val raw = getString(KEY_AUTO_TUNNEL_PLAN) ?: return null
        return runCatching { json.decodeFromString(AutoTunnelConfigDto.serializer(), raw) }
            .getOrNull()
    }

    override suspend fun setLocalLoggingEnabled(enabled: Boolean) =
        put(KEY_LOCAL_LOGGING, enabled.toString())

    override suspend fun getLocalLoggingEnabled(): Boolean = getBoolean(KEY_LOCAL_LOGGING)

    private fun getString(key: String): String? = synchronized(lock) { props.getProperty(key) }

    private fun getBoolean(key: String): Boolean = getString(key)?.toBoolean() == true

    private fun getLong(key: String): Long? = getString(key)?.toLongOrNull()

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
        private const val KEY_LAST_START = "last_start_request"
        private const val KEY_LAST_TUNNEL_ID = "last_active_tunnel_id"
        private const val KEY_RESTORE_ON_BOOT = "restore_on_boot"
        private const val KEY_AUTO_TUNNEL_PLAN = "auto_tunnel_plan"
        private const val KEY_LOCAL_LOGGING = "local_logging"
    }
}

package com.zaneschepke.wireguardautotunnel.daemon.data

import co.touchlab.kermit.Logger
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import org.apache.commons.lang3.SystemUtils

class SettingsDaemonCacheRepository(private val baseCacheDir: Path = getCacheBaseDir()) :
    DaemonCacheRepository {

    private val settings: Settings by lazy {
        val storePathNio = baseCacheDir.resolve(CACHE_FILE_NAME)

        // create cache dir
        if (Files.notExists(baseCacheDir)) {
            Files.createDirectories(baseCacheDir)
        }

        // secure the cache dir for admin/root only
        PermissionsHelper.secureDaemonDataDirectory(baseCacheDir)

        // load data
        val props = Properties()
        if (Files.exists(storePathNio) && Files.size(storePathNio) > 0) {
            Files.newInputStream(storePathNio).use { props.load(it) }
        }

        // save
        PropertiesSettings(props) {
            try {
                FileOutputStream(storePathNio.toFile()).use { output ->
                    props.store(output, "WireGuard AutoTunnel Daemon Cache")
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to save settings to disk" }
            }
        }
    }

    companion object {
        const val CACHE_FILE_NAME = "cache.properties"

        private const val KEY_KS_ENABLED = "killswitch_enabled"
        private const val KEY_KS_BYPASS_LAN = "killswitch_bypass_lan"
        private const val KEY_KS_RESTORE = "killswitch_restore"
        private const val KEY_LAST_TUNNEL = "last_active_tunnel"
        private const val KEY_LAST_TUNNEL_ID = "last_active_tunnel_id"
        private const val KEY_LAST_TUNNEL_NAME = "last_active_tunnel_name"
        private const val KEY_RESTORE_ON_BOOT = "restore_on_boot"

        private fun getCacheBaseDir(): Path {
            return when {
                SystemUtils.IS_OS_MAC_OSX -> Paths.get("/Library/Application Support/wgtunnel")
                SystemUtils.IS_OS_WINDOWS -> Paths.get(System.getenv("PROGRAMDATA") + "\\wgtunnel")
                else -> Paths.get("/var/lib/wgtunnel")
            }
        }
    }

    override suspend fun updateKillSwitchEnabled(enabled: Boolean) =
        settings.set(KEY_KS_ENABLED, enabled)

    override suspend fun getKillSwitchEnabled(): Boolean =
        settings.getBoolean(KEY_KS_ENABLED, false)

    override suspend fun updateKillSwitchBypassLan(enabled: Boolean) =
        settings.set(KEY_KS_BYPASS_LAN, enabled)

    override suspend fun getKillSwitchBypassLan(): Boolean =
        settings.getBoolean(KEY_KS_BYPASS_LAN, false)

    override suspend fun updateKillSwitchRestore(enabled: Boolean) =
        settings.set(KEY_KS_RESTORE, enabled)

    override suspend fun getKillSwitchRestore(): Boolean =
        settings.getBoolean(KEY_KS_RESTORE, false)

    override suspend fun updateLastActiveTunnelConfig(quick: String) =
        settings.set(KEY_LAST_TUNNEL, quick)

    override suspend fun getLastActiveTunnelConfig(): String? =
        settings.getStringOrNull(KEY_LAST_TUNNEL)

    override suspend fun updateLastActiveTunnelId(tunnelId: Long) =
        settings.set(KEY_LAST_TUNNEL_ID, tunnelId)

    override suspend fun getLastActiveTunnelId(): Long? = settings.getLongOrNull(KEY_LAST_TUNNEL_ID)

    override suspend fun updateLastActiveTunnelName(tunnelName: String) =
        settings.set(KEY_LAST_TUNNEL_NAME, tunnelName)

    override suspend fun getLastActiveTunnelName(): String? =
        settings.getStringOrNull(KEY_LAST_TUNNEL_NAME)

    override suspend fun setRestoreTunnelOnBoot(enabled: Boolean) =
        settings.set(KEY_RESTORE_ON_BOOT, enabled)

    override suspend fun getRestoreTunnelOnBoot(): Boolean =
        settings.getBoolean(KEY_RESTORE_ON_BOOT, false)
}

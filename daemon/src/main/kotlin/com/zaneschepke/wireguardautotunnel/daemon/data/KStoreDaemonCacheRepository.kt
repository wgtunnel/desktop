package com.zaneschepke.wireguardautotunnel.daemon.data

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.daemon.data.model.DaemonCacheData
import com.zaneschepke.wireguardautotunnel.daemon.data.model.KillSwitchSettings
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.SystemUtils

class KStoreDaemonCacheRepository(
    private val baseCacheDir: java.nio.file.Path = getCacheBaseDir()
) : DaemonCacheRepository {

    companion object {
        const val CACHE_FILE_NAME = "cache.json"

        private fun getCacheBaseDir(): java.nio.file.Path {
            return when {
                SystemUtils.IS_OS_MAC_OSX -> Paths.get("/Library/Application Support/wgtunnel")
                SystemUtils.IS_OS_WINDOWS -> Paths.get(System.getenv("PROGRAMDATA") + "\\wgtunnel")
                else -> Paths.get("/var/lib/wgtunnel")
            }
        }
    }

    init {
        if (Files.notExists(baseCacheDir)) {
            Files.createDirectories(baseCacheDir)
            setSecurePermissions(baseCacheDir)
        }
    }

    private fun getStore(): KStore<DaemonCacheData> {
        val storePathNio = baseCacheDir.resolve(CACHE_FILE_NAME)
        val storeKPath = Path(storePathNio.toString())

        if (!Files.exists(storePathNio)) {
            Files.createFile(storePathNio)
        }

        if (Files.size(storePathNio) == 0L) {
            val defaultData = DaemonCacheData()
            val defaultJson = Json.encodeToString(defaultData)
            Files.writeString(storePathNio, defaultJson)
        }

        setSecurePermissions(storePathNio)

        return storeOf(file = storeKPath, default = DaemonCacheData())
    }

    private fun setSecurePermissions(path: java.nio.file.Path) {
        val os = System.getProperty("os.name").lowercase()
        try {
            if (!os.contains("win")) {
                val isDirectory = Files.isDirectory(path)
                val permsString =
                    if (isDirectory) "rwx------" else "rw-------" // 700 for dirs, 600 for files
                val perms = PosixFilePermissions.fromString(permsString)
                Files.setPosixFilePermissions(path, perms)
            } else {
                val process =
                    ProcessBuilder(
                            "icacls",
                            path.toString(),
                            "/inheritance:r", // remove inherited permissions
                            "/grant:r",
                            "SYSTEM:(F)", // full control to system
                            "/grant:r",
                            "Administrators:(F)", // full control to admin
                        )
                        .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    Logger.e { "icacls failed with code $exitCode" }
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to set permissions" }
        }
    }

    override suspend fun getKillSwitchSettings(): KillSwitchSettings {
        return getStore().get()?.killSwitch ?: KillSwitchSettings(false, false)
    }

    override suspend fun setKillSwitchSettings(settings: KillSwitchSettings) {
        val store = getStore()
        store.update { current ->
            current?.copy(killSwitch = settings) ?: DaemonCacheData(killSwitch = settings)
        }
    }

    override suspend fun getStartConfigs(): Set<String> {
        return getStore().get()?.startConfigs ?: emptySet()
    }

    override suspend fun setStartConfigs(configs: Set<String>) {
        val store = getStore()
        store.update { current ->
            current?.copy(startConfigs = configs) ?: DaemonCacheData(startConfigs = configs)
        }
    }
}

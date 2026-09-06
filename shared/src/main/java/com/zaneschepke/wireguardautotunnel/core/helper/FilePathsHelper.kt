package com.zaneschepke.wireguardautotunnel.core.helper

import com.zaneschepke.wireguardautotunnel.core.ipc.IPC.SOCKET_FILE_NAME
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object FilePathsHelper {
    const val APP_NAME = "WGTunnel"

    private val osName by lazy { System.getProperty("os.name").lowercase() }
    private val isWindows: Boolean
        get() = osName.contains("win")

    private val isMac: Boolean
        get() = osName.contains("mac")

    fun getDatabaseDir(): File {
        val variant = AppVariant.current
        val home = System.getProperty("user.home")
        return when {
            isWindows -> {
                val appData = System.getenv("APPDATA") ?: "$home\\AppData\\Roaming"
                File("$appData\\${variant.desktopAppName}")
            }
            isMac -> {
                File("$home/Library/Application Support/${variant.desktopAppName}")
            }
            else -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
                File("$xdgDataHome/${variant.linuxFsName}")
            }
        }
    }

    fun getDaemonRuntimeDir(): Path {
        val name = AppVariant.current.linuxFsName
        return when {
            isWindows -> Paths.get(System.getenv("PROGRAMDATA") ?: "C:\\ProgramData", name)
            isMac -> Paths.get("/tmp", name)
            else -> Paths.get("/run", name)
        }
    }

    fun getDaemonSocketPath(): String {
        return getDaemonRuntimeDir().resolve(SOCKET_FILE_NAME).toString()
    }

    fun getDaemonCacheBaseDir(): Path {
        val name = AppVariant.current.linuxFsName
        return when {
            isMac -> Paths.get("/Library/Application Support", name)
            isWindows -> Paths.get(System.getenv("PROGRAMDATA") ?: "C:\\ProgramData", name)
            else -> Paths.get("/var/lib", name)
        }
    }

    fun getAppLogDir(): File = File(getDatabaseDir(), "logs")

    fun getDaemonLogDir(): Path = getDaemonCacheBaseDir().resolve("logs")
}

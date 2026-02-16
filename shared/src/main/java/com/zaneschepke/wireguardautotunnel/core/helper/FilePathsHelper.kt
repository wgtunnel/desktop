package com.zaneschepke.wireguardautotunnel.core.helper

import com.zaneschepke.wireguardautotunnel.core.ipc.IPC.SOCKET_FILE_NAME
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.commons.lang3.SystemUtils

object FilePathsHelper {
    const val APP_NAME = "WGTunnel" // macos convention

    fun getDatabaseDir(): File {
        val home = System.getProperty("user.home")
        return when {
            SystemUtils.IS_OS_WINDOWS -> {
                val appData =
                    System.getenv("APPDATA")
                        ?: "${System.getProperty("user.home")}\\AppData\\Roaming"
                File("$appData\\$APP_NAME")
            }
            SystemUtils.IS_OS_MAC -> {
                File("$home/Library/Application Support/$APP_NAME")
            }
            else -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
                File("$xdgDataHome/${APP_NAME.lowercase()}") // linux lowercase convention
            }
        }
    }

    fun getDaemonSocketPath(): String {
        return when {
            SystemUtils.IS_OS_WINDOWS -> {
                val baseDir = System.getenv("PROGRAMDATA") + "\\wgtunnel"
                "$baseDir\\$SOCKET_FILE_NAME"
            }
            SystemUtils.IS_OS_MAC_OSX -> {
                "/tmp/wgtunnel/$SOCKET_FILE_NAME"
            }
            else -> {
                "/run/wgtunnel/$SOCKET_FILE_NAME"
            }
        }
    }

    fun getDaemonCacheBaseDir(): Path {
        return when {
            SystemUtils.IS_OS_MAC_OSX -> Paths.get("/Library/Application Support/wgtunnel")
            SystemUtils.IS_OS_WINDOWS -> Paths.get(System.getenv("PROGRAMDATA") + "\\wgtunnel")
            else -> Paths.get("/var/lib/wgtunnel")
        }
    }
}

package com.zaneschepke.wireguardautotunnel.core.ipc

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object IPC {
    const val KEY_FILE = "ipc.key"
    const val USER_FOLDER = ".wgtunnel"
    const val SOCKET_FILE_NAME = "daemon.sock"

    fun resolveKeyForUser(user: String): String? {
        if (!user.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            Logger.w { "Invalid username format: $user" }
            return null
        }

        return try {
            val userHome = getUserHome(user)
            val keyPath = Paths.get(userHome, USER_FOLDER, KEY_FILE)

            if (Files.exists(keyPath)) {
                keyPath.toFile().readText().trim().takeIf { it.isNotBlank() }
            } else {
                Logger.w { "IPC key not found for user: $user → $keyPath" }
                null
            }
        } catch (e: Exception) {
            Logger.Companion.e(e) { "Failed to resolve IPC key for user: $user" }
            null
        }
    }

    // should be called by client ONLY
    fun getIPCSecret() : String {
        val ipcFile = File(System.getProperty("user.home"), "${IPC.USER_FOLDER}/${IPC.KEY_FILE}")
        if (!ipcFile.parentFile.exists()) ipcFile.parentFile.mkdirs()

        return if (!ipcFile.exists()) {
            val secret = Crypto.generateRandomBase64(32)
            ipcFile.writeText(secret)
            // Set 600 permissions immediately
            PermissionsHelper.setOwnerOnly(ipcFile.toPath())
            secret
        } else {
            ipcFile.readText()
        }
    }

    private fun getUserHome(user: String): String {
        return when {
            SystemUtils.IS_OS_WINDOWS -> "C:\\Users\\$user"
            SystemUtils.IS_OS_MAC_OSX -> "/Users/$user"
            else -> "/home/$user"
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
}
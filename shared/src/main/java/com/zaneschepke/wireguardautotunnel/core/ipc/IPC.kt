package com.zaneschepke.wireguardautotunnel.core.ipc

import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.io.File

object IPC {

    const val KEY_FILE = "ipc.key"
    const val SOCKET_FILE_NAME = "daemon.sock"

    val userFolder: String
        get() = AppVariant.current.ipcFolder

    private val osName by lazy { System.getProperty("os.name").lowercase() }
    private val isWindows: Boolean
        get() = osName.contains("win")

    private val isMac: Boolean
        get() = osName.contains("mac")

    // should be called by client ONLY
    fun getIPCSecret(): String {
        val ipcFile = ipcKeyFile()
        if (!ipcFile.parentFile.exists()) ipcFile.parentFile.mkdirs()

        return if (!ipcFile.exists()) {
            val secret = Crypto.generateRandomBase64(32)
            ipcFile.writeText(secret)
            PermissionsHelper.setOwnerOnly(ipcFile.toPath())
            secret
        } else {
            ipcFile.readText()
        }
    }

    fun getIpcKeyPath(): String {
        val keyFile = ipcKeyFile()
        if (!keyFile.parentFile.exists()) keyFile.parentFile.mkdirs()

        if (!keyFile.exists()) {
            val secret = Crypto.generateRandomBase64(32)
            keyFile.writeText(secret)
            PermissionsHelper.setOwnerOnly(keyFile.toPath())
        }
        return keyFile.canonicalPath
    }

    // Mirrors SystemHomeDirectory.expectedIpcBaseDir and falls back to ~/.local/share if no
    // session manager created a runtime dir, same as the validator.
    private fun ipcBaseDir(): File {
        val home = System.getProperty("user.home")
        return when {
            isWindows -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming")
            isMac -> File("$home/Library/Application Support")
            else -> {
                val runtimeDir =
                    File(System.getenv("XDG_RUNTIME_DIR") ?: "/run/user/${unixUid()}")
                if (runtimeDir.isDirectory) runtimeDir else File("$home/.local/share")
            }
        }
    }

    private fun unixUid(): String =
        runCatching {
                val process = ProcessBuilder("id", "-u").start()
                process.waitFor()
                process.inputStream.bufferedReader().readText().trim()
            }
            .getOrDefault("0")

    private fun ipcKeyFile(): File {
        val dir = File(ipcBaseDir(), AppVariant.current.ipcFolder)
        return File(dir, KEY_FILE)
    }
}

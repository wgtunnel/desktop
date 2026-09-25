package com.zaneschepke.wireguardautotunnel.core.ipc

import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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

        if (!ipcFile.exists()) return createKey(ipcFile)

        // 2.2.0 could leave a Windows key the daemon rejects: unreadable to this user (made
        // elevated, so locked to Administrators) or with inherited permissions others can read.
        // The daemon re-reads the secret on every request, so replace it (a new value, since the
        // old one may have been exposed).
        if (isWindows && keyNeedsReplacing(ipcFile) && ipcFile.delete()) {
            return createKey(ipcFile)
        }
        return ipcFile.readText()
    }

    @Volatile private var permissionsVerified = false

    private fun keyNeedsReplacing(ipcFile: File): Boolean {
        if (!Files.isReadable(ipcFile.toPath())) return true
        // Spawns icacls, so check once per process (this runs on every request).
        if (permissionsVerified) return false
        permissionsVerified = PermissionsHelper.isOwnerOnly(ipcFile.toPath())
        return !permissionsVerified
    }

    private fun createKey(ipcFile: File): String {
        val secret = Crypto.generateRandomBase64(32)
        ipcFile.writeText(secret)
        PermissionsHelper.setOwnerOnly(ipcFile.toPath())
        return secret
    }

    // should be called by client ONLY
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
            isWindows -> windowsIpcBaseDir(home)
            isMac -> File("$home/Library/Application Support")
            else -> {
                val runtimeDir =
                    File(System.getenv("XDG_RUNTIME_DIR") ?: "/run/user/${unixUid()}")
                if (runtimeDir.isDirectory) runtimeDir else File("$home/.local/share")
            }
        }
    }

    // Roaming AppData, unless it's redirected off the local profile (share, other drive),
    // which the SYSTEM daemon can't read, then Local AppData. Unredirected setups are unchanged.
    private fun windowsIpcBaseDir(home: String): File {
        val roaming = File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming")
        val profile = System.getenv("USERPROFILE") ?: home
        val roamingIsLocal =
            runCatching { roaming.toPath().normalize().startsWith(Paths.get(profile).normalize()) }
                .getOrDefault(false)
        return if (roamingIsLocal) roaming
        else File(System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local")
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

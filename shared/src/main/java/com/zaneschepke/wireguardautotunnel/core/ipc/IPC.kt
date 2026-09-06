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

    private fun ipcKeyFile(): File {
        val dir = File(System.getProperty("user.home"), AppVariant.current.ipcFolder)
        return File(dir, KEY_FILE)
    }
}

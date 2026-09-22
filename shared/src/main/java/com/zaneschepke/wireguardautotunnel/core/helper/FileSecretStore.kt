package com.zaneschepke.wireguardautotunnel.core.helper

import java.io.File

/**
 * Raw file secret storage for environments where no OS keyring daemon is available
 */
object FileSecretStore {
    private fun secretsDir(): File = File(FilePathsHelper.getDatabaseDir(), "secrets")

    private fun sanitize(raw: String): String = raw.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun secretFile(service: String, name: String): File =
        File(secretsDir(), "${sanitize(service)}_${sanitize(name)}")

    fun put(service: String, name: String, value: String) {
        val dir = secretsDir()
        if (!dir.exists()) dir.mkdirs()
        PermissionsHelper.setOwnerOnly(dir.toPath())
        val file = secretFile(service, name)
        file.writeText(value)
        PermissionsHelper.setOwnerOnly(file.toPath())
    }

    fun get(service: String, name: String): String? {
        val file = secretFile(service, name)
        return if (file.exists()) file.readText() else null
    }

    fun delete(service: String, name: String) {
        secretFile(service, name).delete()
    }
}

package com.zaneschepke.wireguardautotunnel.core.ipc

import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.SystemHomeDirectory
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Resolves and validates the IPC key file a client claims to be using, without trusting
 * the claimed path itself. The check that matters is [Result.Trusted] requiring the file to sit
 * exactly at the owning account's expected IPC directory (see
 * [SystemHomeDirectory.expectedIpcBaseDir]), derived from the OS's user database.
 */
object IpcKeyFileValidator {

    sealed interface Result {
        data class Trusted(val secret: String) : Result

        data class Rejected(val reason: String) : Result
    }

    fun resolve(keyPathStr: String): Result {
        // Resolves symlinks too, unlike normalize()/toAbsolutePath() - a symlink pointing outside
        // the expected structure would otherwise sail through the checks below.
        val keyPath =
            try {
                Paths.get(keyPathStr).toRealPath()
            } catch (_: Exception) {
                return Result.Rejected("Invalid or non-existent key path: $keyPathStr")
            }

        val keyFile = keyPath.toFile()

        if (
            keyFile.name != IPC.KEY_FILE || keyFile.parentFile?.name != AppVariant.current.ipcFolder
        ) {
            return Result.Rejected("Path does not match expected structure: $keyPath")
        }
        if (!keyFile.isFile) {
            return Result.Rejected("Key file does not exist: $keyPath")
        }
        if (!PermissionsHelper.isOwnerOnly(keyPath)) {
            return Result.Rejected("Key file permissions are not 0600: $keyPath")
        }

        val ownerAccount =
            try {
                // Windows principal names can come back as "DOMAIN\user"; only the account name
                // is needed to look up its registered profile.
                Files.getOwner(keyPath).name.substringAfterLast('\\')
            } catch (_: Exception) {
                return Result.Rejected("Could not determine owner of key file: $keyPath")
            }

        val expectedBase =
            SystemHomeDirectory.expectedIpcBaseDir(keyPath, ownerAccount)
                ?: return Result.Rejected(
                    "Could not resolve expected IPC base directory for owner '$ownerAccount'"
                )

        val expectedKeyPath = runCatching {
            expectedBase.toRealPath()
        }
            .getOrElse { expectedBase.normalize() }
            .resolve(AppVariant.current.ipcFolder)
            .resolve(IPC.KEY_FILE)

        if (keyPath != expectedKeyPath) {
            return Result.Rejected(
                "Key file is not under owner '$ownerAccount''s expected IPC directory: " +
                    "$keyPath (expected $expectedKeyPath)"
            )
        }

        val secret = keyFile.readText().trim()
        if (secret.isBlank()) return Result.Rejected("Empty key file: $keyPath")

        return Result.Trusted(secret)
    }
}

package com.zaneschepke.wireguardautotunnel.core.ipc

import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.SystemHomeDirectory
import com.zaneschepke.wireguardautotunnel.core.helper.WindowsSid
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolves and validates the IPC key file a client claims to be using, without trusting
 * the claimed path itself. The check that matters is [Result.Trusted] requiring the file to sit
 * exactly at the owning account's expected IPC directory (see
 * [SystemHomeDirectory.expectedIpcBaseDir]), derived from the OS's user database.
 */
object IpcKeyFileValidator {

    private val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    sealed interface Result {
        data class Trusted(val secret: String) : Result

        data class Rejected(val reason: String) : Result
    }

    fun resolve(keyPathStr: String): Result {
        // Before any filesystem access, as SYSTEM, resolving a client-supplied \\host\share path
        // would authenticate to that host as the machine account. A key is never on a UNC path.
        if (isWindows && (keyPathStr.startsWith("\\\\") || keyPathStr.startsWith("//"))) {
            return Result.Rejected("UNC and device paths are not accepted for the IPC key")
        }

        // Resolves symlinks too, unlike normalize()/toAbsolutePath() - a symlink pointing outside
        // the expected structure would otherwise sail through the checks below.
        val keyPath =
            try {
                Paths.get(keyPathStr).toRealPath()
            } catch (_: Exception) {
                return Result.Rejected("Invalid or non-existent key path: $keyPathStr")
            }

        val keyFile = keyPath.toFile()

        // NTFS is case-insensitive but case preserving. If a same name but differently cased
        // directory already exists Java's canonical/real path resolves to whatever casing is
        // actually on disk, which can differ from this string literal. String != is always
        // case-sensitive regardless of platform, so match the OS's own filesystem semantics here
        // instead of assuming Linux/macOS-style case sensitivity everywhere.
        if (
            !keyFile.name.equals(IPC.KEY_FILE, ignoreCase = isWindows) ||
                !(keyFile.parentFile?.name).equals(AppVariant.current.ipcFolder, ignoreCase = isWindows)
        ) {
            return Result.Rejected(
                "Path does not match expected structure: $keyPath " +
                    "(name=${keyFile.name}, expected=${IPC.KEY_FILE}; " +
                    "parent=${keyFile.parentFile?.name}, expected=${AppVariant.current.ipcFolder})"
            )
        }
        if (!keyFile.isFile) {
            return Result.Rejected("Key file does not exist: $keyPath")
        }
        if (!PermissionsHelper.isOwnerOnly(keyPath)) {
            return Result.Rejected("Key file permissions are not 0600: $keyPath")
        }

        val ownershipFailure =
            if (isWindows) validateWindowsOwnership(keyPath) else validatePosixOwnership(keyPath)
        if (ownershipFailure != null) return ownershipFailure

        val secret = keyFile.readText().trim()
        if (secret.isBlank()) return Result.Rejected("Empty key file: $keyPath")

        return Result.Trusted(secret)
    }

    private fun validatePosixOwnership(keyPath: Path): Result.Rejected? {
        val ownerAccount =
            try {
                Files.getOwner(keyPath).name.substringAfterLast('\\')
            } catch (_: Exception) {
                return Result.Rejected("Could not determine owner of key file: $keyPath")
            }

        val expectedBase =
            SystemHomeDirectory.expectedIpcBaseDir(ownerAccount)
                ?: return Result.Rejected(
                    "Could not resolve expected IPC base directory for owner '$ownerAccount'"
                )

        val expectedKeyPath = expectedKeyPathUnder(expectedBase)

        if (keyPath != expectedKeyPath) {
            return Result.Rejected(
                "Key file is not under owner '$ownerAccount''s expected IPC directory: " +
                    "$keyPath (expected $expectedKeyPath)"
            )
        }
        return null
    }

    /**
     * The owner can't locate the profile on Windows (an elevated process's files are owned by
     * Administrators, a group with no profile), so find the profile the file sits in and require
     * its owner to be that account or Administrators. Accepting Administrators is safe as an admin
     * can already control the daemon and read any key.
     */
    private fun validateWindowsOwnership(keyPath: Path): Result.Rejected? {
        val profiles =
            SystemHomeDirectory.windowsProfiles()
                ?: return Result.Rejected(
                    "Could not read the registered Windows user profiles (reg query ProfileList failed)"
                )

        val profileSid =
            matchProfileSid(keyPath, profiles)
                ?: return Result.Rejected(
                    "Key file is not at the IPC location of any registered user profile: " +
                        "$keyPath (checked ${profiles.size} profiles)"
                )

        val ownerSid =
            WindowsSid.ownerOf(keyPath)
                ?: return Result.Rejected(
                    "Could not determine the owning SID of key file (SID lookup failed): $keyPath"
                )

        if (!isAllowedOwner(ownerSid, profileSid)) {
            return Result.Rejected(
                "Key file $keyPath is owned by $ownerSid, expected the profile's account " +
                    "($profileSid) or Administrators (${WindowsSid.ADMINISTRATORS})"
            )
        }
        return null
    }

    /**
     * SID of the registered profile whose IPC key path is exactly [keyPath]. Accepts Roaming or
     * Local AppData. The client falls back to Local when Roaming is redirected.
     */
    internal fun matchProfileSid(keyPath: Path, profiles: Map<String, Path>): String? =
        profiles.entries
            .firstOrNull { (_, profileDir) ->
                listOf("Roaming", "Local").any { appData ->
                    val expected =
                        expectedKeyPathUnder(profileDir.resolve("AppData").resolve(appData))
                    // NTFS is case-insensitive.
                    keyPath.toString().equals(expected.toString(), ignoreCase = true)
                }
            }
            ?.key

    internal fun isAllowedOwner(ownerSid: String, profileSid: String): Boolean =
        ownerSid.equals(profileSid, ignoreCase = true) ||
            ownerSid.equals(WindowsSid.ADMINISTRATORS, ignoreCase = true)

    private fun expectedKeyPathUnder(base: Path): Path =
        runCatching { base.toRealPath() }
            .getOrElse { base.normalize() }
            .resolve(AppVariant.current.ipcFolder)
            .resolve(IPC.KEY_FILE)
}

package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.policy.stopAtAttempts
import com.github.michaelbull.retry.retry
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PermissionsHelper {
    private val log = Logger.withTag("Permissions")

    private val osName by lazy { System.getProperty("os.name").lowercase() }
    private val isWindows: Boolean
        get() = osName.contains("win")

    val socketRetryPolicy =
        binaryExponentialBackoff<Throwable>(min = 10L, max = 250L) + stopAtAttempts(25)

    // unix
    const val WORLD_WRITABLE_OCTAL = "666"
    const val WORLD_READWRITE_SYMBOLIC = "rw-rw-rw-"
    const val OWNER_FULL_CONTROL_OCTAL = "755"
    const val OWNER_FULL_CONTROL_SYMBOLIC = "rwxr-xr-x"
    const val OWNER_ONLY_PRIVATE_FILE = "rw-------"
    const val OWNER_ONLY_PRIVATE_DIR = "rwx------"

    // windows universal SIDs
    private const val SID_SYSTEM = "*S-1-5-18"
    private const val SID_ADMINISTRATORS = "*S-1-5-32-544"
    private const val SID_USERS = "*S-1-5-32-545"

    // windows permission flags
    private const val WIN_DIR_MODIFY_INHERIT = ":(OI)(CI)(M)"
    private const val WIN_FULL_CONTROL_INHERIT = ":(OI)(CI)(F)"
    private const val WIN_READY_ONLY = ":(R)"
    private const val WIN_FULL_CONTROL = ":(F)"

    // windows icacls
    private const val ICACLS = "icacls"
    private const val WIN_GRANT = "/grant"
    private const val WIN_GRANT_REPLACE = "/grant:r"
    private const val WIN_INHERIT_REPLACE = "/inheritance:r"

    fun setupDirectoryPermissionsUnix(runtimeDirPath: String) {
        val path = Paths.get(runtimeDirPath)

        if (Files.exists(path)) {
            try {
                Files.setPosixFilePermissions(
                    path,
                    PosixFilePermissions.fromString(OWNER_FULL_CONTROL_SYMBOLIC),
                )
                log.i { "Successfully set daemon data directory permission" }
            } catch (e: Exception) {
                log.e { "POSIX native permissions failed: ${e.message} → falling back to chmod" }
                try {
                    val exitCode =
                        ProcessBuilder("chmod", OWNER_FULL_CONTROL_OCTAL, runtimeDirPath)
                            .start()
                            .waitFor()

                    if (exitCode == 0) {
                        log.i { "Successfully set directory permissions using chmod" }
                    } else {
                        log.e { "chmod failed with exit code $exitCode" }
                    }
                } catch (chmodEx: Exception) {
                    log.e { "Failed to execute chmod: ${chmodEx.message}" }
                }
            }
        } else {
            log.w { "Runtime directory $runtimeDirPath not found" }
        }
    }

    fun secureDaemonDataDirectory(path: Path) {
        val pathString = path.toString()
        try {
            if (isWindows) {
                val process =
                    ProcessBuilder(
                            ICACLS,
                            pathString,
                            WIN_INHERIT_REPLACE,
                            WIN_GRANT_REPLACE,
                            "$SID_SYSTEM$WIN_FULL_CONTROL_INHERIT",
                            WIN_GRANT_REPLACE,
                            "$SID_ADMINISTRATORS$WIN_FULL_CONTROL_INHERIT",
                        )
                        .start()

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    log.i { "Successfully secured Windows directory: $pathString" }
                    logWindowsACLs(pathString)
                } else {
                    val error = process.errorStream.bufferedReader().use { it.readText() }
                    log.e { "Failed to secure Windows directory: $error" }
                }
            } else {
                try {
                    Files.setPosixFilePermissions(
                        path,
                        PosixFilePermissions.fromString(OWNER_ONLY_PRIVATE_DIR),
                    )
                    log.i { "Successfully set POSIX permissions for directory: $pathString" }
                } catch (e: Exception) {
                    log.e {
                        "POSIX native permissions failed: ${e.message} → falling back to chmod"
                    }
                    val exitCode = ProcessBuilder("chmod", "700", pathString).start().waitFor()
                    if (exitCode == 0) {
                        log.i { "Successfully set directory permissions using chmod: $pathString" }
                    } else {
                        log.e { "chmod failed with exit code $exitCode for: $pathString" }
                    }
                }
                val finalPerms = Files.getPosixFilePermissions(path)
                log.i { "Final directory permissions: $finalPerms for $pathString" }
            }
        } catch (e: Exception) {
            log.e(e) { "Error securing directory: $pathString" }
        }
    }

    fun setupDirectoryPermissionsWindows(runtimeDirPath: String) {
        try {
            val process =
                ProcessBuilder(
                        ICACLS,
                        runtimeDirPath,
                        WIN_GRANT,
                        "$SID_USERS$WIN_DIR_MODIFY_INHERIT",
                        WIN_GRANT,
                        "$SID_SYSTEM$WIN_FULL_CONTROL_INHERIT",
                        WIN_GRANT,
                        "$SID_ADMINISTRATORS$WIN_FULL_CONTROL_INHERIT",
                    )
                    .start()

            if (process.waitFor() != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                log.e { "icacls directory setup failed: $error" }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to set Windows directory ACLs" }
        }
    }

    suspend fun setupSocketPermissionsWithPollUnix(socketPath: String) =
        withContext(Dispatchers.IO) {
            val socketFile = File(socketPath)

            runCatching {
                retry(socketRetryPolicy) {
                    if (!socketFile.exists()) {
                        throw FileNotFoundException("Socket $socketPath not found yet")
                    }
                    setupSocketPermissionsUnix(socketPath)
                }

                val socketPerms = Files.getPosixFilePermissions(Paths.get(socketPath))
                log.i { "Final socket permissions: $socketPerms" }
            }
                .onFailure {
                    log.e {
                        "Socket $socketPath failed to appear. Daemon likely failed to start: ${it.message}"
                    }
                }
        }

    suspend fun setupSocketPermissionsWithPollWindows(socketPath: String) =
        withContext(Dispatchers.IO) {
            val socketFile = File(socketPath)
            runCatching {
                retry(socketRetryPolicy) {
                    if (!socketFile.exists()) throw FileNotFoundException("Socket not found yet")
                    setupDirectoryPermissionsWindows(socketPath)
                }
                logWindowsACLs(socketPath)
            }
                .onFailure {
                    log.e { "Socket $socketPath failed to appear on Windows: ${it.message}" }
                }
        }

    fun setupSocketPermissionsUnix(socketPath: String) {
        val path = Paths.get(socketPath)
        try {
            Files.setPosixFilePermissions(
                path,
                PosixFilePermissions.fromString(WORLD_READWRITE_SYMBOLIC),
            )
            log.i { "Successfully set socket permissions to 0666" }
        } catch (e: Exception) {
            log.e { "POSIX native permissions failed: ${e.message} → falling back to chmod" }

            try {
                val exitCode =
                    ProcessBuilder("chmod", WORLD_WRITABLE_OCTAL, socketPath).start().waitFor()

                if (exitCode == 0) {
                    log.i { "Successfully set socket permissions using chmod" }
                } else {
                    log.e { "chmod failed with exit code $exitCode" }
                    throw IllegalStateException("chmod exited with non-zero status")
                }
            } catch (chmodEx: Exception) {
                log.e { "All POSIX methods failed: ${chmodEx.message} → using JVM fallback" }

                //  try file API
                val socketFile = path.toFile()
                val readOk = socketFile.setReadable(true, false)
                val writeOk = socketFile.setWritable(true, false)

                if (readOk && writeOk) {
                    log.w { "Applied weak Java fallback permissions (readable/writable for all)" }
                } else {
                    log.e { "Failed to set any permissions on socket $socketPath" }
                }
            }
        }
    }

    fun setOwnerOnly(path: Path) {
        try {
            if (isWindows) {
                applyWindowsOwnerOnlyPermissions(path)
            } else {
                applyPosixOwnerOnlyPermissions(path)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to set permissions for: $path" }
        }
    }

    private fun applyPosixOwnerOnlyPermissions(path: Path) {
        val isDir = Files.isDirectory(path)
        val permsString = if (isDir) OWNER_ONLY_PRIVATE_DIR else OWNER_ONLY_PRIVATE_FILE
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permsString))
    }

    private fun applyWindowsOwnerOnlyPermissions(path: Path) {
        val currentUser = System.getProperty("user.name")

        try {
            val process =
                ProcessBuilder(
                        ICACLS,
                        path.toString(),
                        WIN_INHERIT_REPLACE,
                        WIN_GRANT_REPLACE,
                        "$SID_SYSTEM$WIN_READY_ONLY",
                        WIN_GRANT_REPLACE,
                        "$SID_ADMINISTRATORS$WIN_READY_ONLY",
                        WIN_GRANT_REPLACE,
                        "$currentUser$WIN_FULL_CONTROL",
                    )
                    .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                log.e { "icacls IPC key setup failed: $error" }
            }
        } catch (e: Exception) {
            log.e(e) { "Error applying read-only Windows perms to IPC key" }
        }
    }

    fun isOwnerOnly(path: Path): Boolean {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.w { "isOwnerOnly check failed basic validation: $path" }
            return false
        }

        return try {
            if (isWindows) {
                isOwnerOnlyWindows(path)
            } else {
                isOwnerOnlyPosix(path)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to verify owner-only permissions for: $path" }
            false
        }
    }

    private fun isOwnerOnlyPosix(path: Path): Boolean {
        return try {
            val perms = Files.getPosixFilePermissions(path)
            // Exact match to what setOwnerOnly applies
            perms == PosixFilePermissions.fromString(OWNER_ONLY_PRIVATE_FILE)
        } catch (e: Exception) {
            log.w(e) { "POSIX permission read failed for $path" }
            false
        }
    }

    private fun isOwnerOnlyWindows(path: Path): Boolean {
        return try {
            val process = ProcessBuilder(ICACLS, path.toString()).start()
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                log.w { "icacls failed (exit $exitCode) checking $path" }
                return false
            }

            val lowerOutput = output.lowercase()

            // One principal user with full control
            val hasFullControl = lowerOutput.contains(":(f)")

            // Dangerous groups should have no access
            val dangerousGroups =
                listOf(
                    "everyone",
                    "s-1-1-0",
                    "users",
                    "builtin\\users",
                    "s-1-5-32-545",
                    "guests",
                    "s-1-5-32-546",
                    "authenticated users",
                    "s-1-5-11",
                )

            val hasDangerousWrite = dangerousGroups.any { group ->
                lowerOutput.contains(group) &&
                    (lowerOutput.contains("$group:(f)") ||
                        lowerOutput.contains("$group:(m)") ||
                        lowerOutput.contains("$group:(w)"))
            }

            if (!hasFullControl) {
                log.w { "IPC key has no principal with Full Control: $path" }
            }
            if (hasDangerousWrite) {
                log.w { "Dangerous group has write access on IPC key: $path" }
            }

            val isValid = hasFullControl && !hasDangerousWrite

            if (isValid) {
                log.i { "IPC key ownership verified successfully: $path" }
            }

            isValid
        } catch (e: Exception) {
            log.w(e) { "Windows ACL check failed for $path" }
            false
        }
    }

    private fun logWindowsACLs(path: String) {
        runCatching {
            val output =
                ProcessBuilder(ICACLS, path).start().inputStream.bufferedReader().readText()
            log.i { "Final ACLs for $path: $output" }
        }
    }
}

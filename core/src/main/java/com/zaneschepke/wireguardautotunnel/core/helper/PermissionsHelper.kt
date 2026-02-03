package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.policy.stopAtAttempts
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

object PermissionsHelper {

    val socketRetryPolicy = binaryExponentialBackoff<Throwable>(min = 10L, max = 250L) + stopAtAttempts(25)

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
    private const val SID_CREATOR_OWNER = "*S-1-3-0"

    // windows permission flags
    private const val WIN_DIR_MODIFY_INHERIT = ":(OI)(CI)(M)"
    private const val WIN_FULL_CONTROL_INHERIT = ":(OI)(CI)(F)"

    fun setupDirectoryPermissionsUnix(runtimeDirPath: String) {
        val path = Paths.get(runtimeDirPath)

        if (Files.exists(path)) {
            try {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(OWNER_FULL_CONTROL_SYMBOLIC))
                Logger.i { "Successfully set directory permissions to " }
            } catch (e: Exception) {
                Logger.e { "POSIX native permissions failed: ${e.message} → falling back to chmod" }
                try {
                    val exitCode = ProcessBuilder("chmod", OWNER_FULL_CONTROL_OCTAL, runtimeDirPath)
                        .start()
                        .waitFor()

                    if (exitCode == 0) {
                        Logger.i { "Successfully set directory permissions using chmod" }
                    } else {
                        Logger.e { "chmod failed with exit code $exitCode" }
                    }
                } catch (chmodEx: Exception) {
                    Logger.e { "Failed to execute chmod: ${chmodEx.message}" }
                }
            }
        } else {
            Logger.w { "Runtime directory $runtimeDirPath not found" }
        }
    }

    fun setupDirectoryPermissionsWindows(runtimeDirPath: String) {
        try {
            val process = ProcessBuilder(
                "icacls", runtimeDirPath,
                "/grant", "$SID_USERS$WIN_DIR_MODIFY_INHERIT",
                "/grant", "$SID_SYSTEM$WIN_FULL_CONTROL_INHERIT",
                "/grant", "$SID_ADMINISTRATORS$WIN_FULL_CONTROL_INHERIT"
            ).start()

            if (process.waitFor() != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                Logger.e { "icacls directory setup failed: $error" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to set Windows directory ACLs" }
        }
    }

    suspend fun setupSocketPermissionsWithPollUnix(socketPath: String) = withContext(Dispatchers.IO) {
        val socketFile = File(socketPath)

        runCatching {
            retry(socketRetryPolicy) {
                if (!socketFile.exists()) {
                    throw FileNotFoundException("Socket $socketPath not found yet")
                }
                setupSocketPermissionsUnix(socketPath)
            }

            val socketPerms = Files.getPosixFilePermissions(Paths.get(socketPath))
            Logger.i { "Final socket permissions: $socketPerms" }

        }.onFailure {
            Logger.e { "Socket $socketPath failed to appear. Daemon likely failed to start: ${it.message}" }
        }
    }

    suspend fun setupSocketPermissionsWithPollWindows(socketPath: String) = withContext(Dispatchers.IO) {
        val socketFile = File(socketPath)
        runCatching {
            retry(socketRetryPolicy) {
                if (!socketFile.exists()) throw FileNotFoundException("Socket not found yet")
                setupDirectoryPermissionsWindows(socketPath)
            }
            logWindowsACLs(socketPath)
        }.onFailure {
            Logger.e { "Socket $socketPath failed to appear on Windows: ${it.message}" }
        }
    }



    fun setupSocketPermissionsUnix(socketPath: String) {
        val path = Paths.get(socketPath)
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(WORLD_READWRITE_SYMBOLIC))
            Logger.i { "Successfully set socket permissions to 0666" }
        } catch (e: Exception) {
            Logger.e { "POSIX native permissions failed: ${e.message} → falling back to chmod" }

            try {
                val exitCode = ProcessBuilder("chmod", WORLD_WRITABLE_OCTAL, socketPath)
                    .start()
                    .waitFor()

                if (exitCode == 0) {
                    Logger.i { "Successfully set socket permissions using chmod" }
                } else {
                    Logger.e { "chmod failed with exit code $exitCode" }
                    throw IllegalStateException("chmod exited with non-zero status")
                }
            } catch (chmodEx: Exception) {
                Logger.e { "All POSIX methods failed: ${chmodEx.message} → using JVM fallback" }

                //  try file API
                val socketFile = path.toFile()
                val readOk = socketFile.setReadable(true, false)
                val writeOk = socketFile.setWritable(true, false)

                if (readOk && writeOk) {
                    Logger.w { "Applied weak Java fallback permissions (readable/writable for all)" }
                } else {
                    Logger.e { "Failed to set any permissions on socket $socketPath" }
                }
            }
        }
    }

    fun setOwnerOnly(path: Path) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                applyWindowsOwnerOnlyPermissions(path)
            } else {
                applyPosixOwnerOnlyPermissions(path)
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to set permissions for: $path" }
        }
    }

    private fun applyPosixOwnerOnlyPermissions(path: Path) {
        val isDir = Files.isDirectory(path)
        val permsString = if (isDir) OWNER_ONLY_PRIVATE_DIR else OWNER_ONLY_PRIVATE_FILE
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permsString))
    }

    private fun applyWindowsOwnerOnlyPermissions(path: Path) {
        try {
            val process = ProcessBuilder(
                "icacls", path.toString(),
                "/inheritance:r", // remove inherited perms
                "/grant:r", "$SID_SYSTEM$WIN_FULL_CONTROL_INHERIT",
                "/grant:r", "$SID_CREATOR_OWNER$WIN_FULL_CONTROL_INHERIT",
                "/grant:r", "$SID_ADMINISTRATORS$WIN_FULL_CONTROL_INHERIT"
            ).start()

            if (process.waitFor() != 0) {
                Logger.e { "icacls owner-only failed for $path" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Error applying owner-only Windows perms" }
        }
    }
    private fun logWindowsACLs(path: String) {
        runCatching {
            val output = ProcessBuilder("icacls", path).start().inputStream.bufferedReader().readText()
            Logger.i { "Final ACLs for $path: $output" }
        }
    }

}
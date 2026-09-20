package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Resolves a local OS account's home directory from the system's own user database, independent of
 * anything a caller might claim. Used to verify that a file purporting to belong to a given account
 * actually lives where that account's real home directory is registered, rather than trusting a
 * caller supplied path.
 */
object SystemHomeDirectory {
    private val log = Logger.withTag("SystemHomeDirectory")

    private val osName by lazy { System.getProperty("os.name").lowercase() }
    private val isWindows: Boolean
        get() = osName.contains("win")

    private val isMac: Boolean
        get() = osName.contains("mac")

    /** Returns [ownerAccount]'s registered home directory, or null if it can't be determined. */
    fun forOwner(keyPath: Path, ownerAccount: String): Path? =
        when {
            isWindows -> windowsProfilePath(keyPath)
            isMac -> macHomeDir(ownerAccount)
            else -> linuxHomeDir(ownerAccount)
        }

    private fun runCommand(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        process.inputStream.bufferedReader().readText().trim()
    }
        .onFailure { log.w(it) { "Command failed: ${command.joinToString(" ")}" } }
        .getOrNull()

    // `getent` goes through NSS (covers local /etc/passwd accounts as well as
    // LDAP/sssd/winbind-backed ones), unlike parsing /etc/passwd directly.
    private fun linuxHomeDir(username: String): Path? {
        val line = runCommand("getent", "passwd", username) ?: return null
        val fields = line.split(":")
        if (fields.size < 6 || fields[0] != username) return null
        return fields[5].takeIf { it.isNotBlank() }?.let { Paths.get(it) }
    }

    private fun macHomeDir(username: String): Path? {
        val output = runCommand("dscl", ".", "-read", "/Users/$username", "NFSHomeDirectory")
        val path = output?.substringAfter("NFSHomeDirectory:", "")?.trim()
        return path?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
    }

    private fun windowsProfilePath(keyPath: Path): Path? {
        val sid = WindowsSid.ownerOf(keyPath) ?: return null

        val output =
            runCommand(
                "reg",
                "query",
                "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\$sid",
                "/v",
                "ProfileImagePath",
            ) ?: return null

        val match =
            Regex("ProfileImagePath\\s+REG(?:_EXPAND)?_SZ\\s+(.+)").find(output) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }?.let { Paths.get(it) }
    }
}

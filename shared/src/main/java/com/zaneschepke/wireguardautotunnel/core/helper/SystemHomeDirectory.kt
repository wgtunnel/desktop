package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
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

    /**
     * Base directory for user's IPC key folder using the per user XDG
     * runtime dir on Linux and falls back to ~/.local/share if no session manager created one.
     * On macOS, the registered home dir plus the platform's app data suffix.
     *
     * Not used on Windows (see [windowsProfiles]).
     */
    fun expectedIpcBaseDir(ownerAccount: String): Path? =
        when {
            isWindows -> null
            isMac -> macHomeDir(ownerAccount)?.resolve("Library")?.resolve("Application Support")
            else ->
                linuxRuntimeDir(ownerAccount)?.takeIf { Files.isDirectory(it) }
                    ?: linuxHomeDir(ownerAccount)?.resolve(".local")?.resolve("share")
        }

    // Same NSS-backed lookup as linuxHomeDir and the uid is fields[2] of the passwd line.
    private fun linuxRuntimeDir(username: String): Path? {
        val line = runCommand("getent", "passwd", username) ?: return null
        val fields = line.split(":")
        if (fields.size < 6 || fields[0] != username) return null
        val uid = fields[2].takeIf { it.isNotBlank() } ?: return null
        return Paths.get("/run/user/$uid")
    }

    private fun runCommand(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        // Drained while waiting as a full pipe buffer (~4 KB on Windows) blocks the child until timeout.
        val output = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        output.get(5, TimeUnit.SECONDS).trim()
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

    /**
     * Every registered profile (SID to directory), or null if unreadable. Not looked up by file
     * owner, which is a group (with no profile) for files made by an elevated process.
     */
    fun windowsProfiles(): Map<String, Path>? {
        val queries =
            listOf(
                // chcp 65001: reg's default OEM output garbles non-ASCII names ("Ł" becomes "L").
                // /s: cmd strips only the outer quotes.
                arrayOf(
                    "cmd",
                    "/d",
                    "/s",
                    "/c",
                    "\"chcp 65001 >nul & reg query \"$WINDOWS_PROFILE_LIST_KEY\" /s /v ProfileImagePath\"",
                ),
                // Fallback if cmd is unavailable (fine for ASCII paths).
                arrayOf("reg", "query", WINDOWS_PROFILE_LIST_KEY, "/s", "/v", "ProfileImagePath"),
            )
        for (query in queries) {
            val output = runCommand(*query) ?: continue
            val profiles =
                parseWindowsProfileList(output) { name ->
                    System.getenv().entries
                        .firstOrNull { it.key.equals(name, ignoreCase = true) }
                        ?.value
                }
            if (profiles.isNotEmpty()) return profiles
        }
        return null
    }

    private const val WINDOWS_PROFILE_LIST_KEY =
        "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList"

    private val PROFILE_SID_KEY = Regex("^S-1-\\d+(?:-\\d+)+$")
    private val PROFILE_IMAGE_PATH = Regex("^ProfileImagePath\\s+REG_(?:EXPAND_)?SZ\\s+(.+)$")
    private val ENV_VAR = Regex("%([^%]+)%")

    /**
     * Parses `reg query <ProfileList> /s /v ProfileImagePath`. [env] expands `%NAME%`, which reg
     * prints unexpanded for REG_EXPAND_SZ.
     */
    internal fun parseWindowsProfileList(output: String, env: (String) -> String?): Map<String, Path> {
        val profiles = mutableMapOf<String, Path>()
        var currentSid: String? = null
        for (raw in output.lines()) {
            val line = raw.trim()
            if (line.startsWith("HKEY_", ignoreCase = true)) {
                // "<sid>.bak" keys are broken profiles.
                currentSid = line.substringAfterLast('\\').takeIf { PROFILE_SID_KEY.matches(it) }
                continue
            }
            val sid = currentSid ?: continue
            val rawPath = PROFILE_IMAGE_PATH.find(line)?.groupValues?.get(1)?.trim() ?: continue
            val expanded = ENV_VAR.replace(rawPath) { env(it.groupValues[1]) ?: it.value }
            runCatching { Paths.get(expanded) }.getOrNull()?.let { profiles[sid] = it }
        }
        return profiles
    }
}

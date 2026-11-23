package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Resolves a file's owning SID directly from its ACL, without ever needing to know or pass the
 * owning account's *name*. Account names containing non-ASCII characters can come back corrupted.
 */
object WindowsSid {
    private val log = Logger.withTag("WindowsSid")

    /** Built-in Administrators group: owns files created by an elevated process. */
    const val ADMINISTRATORS = "S-1-5-32-544"

    private val SID_PATTERN = Regex("S-1-\\d+(?:-\\d+)+")
    private val SID_EXACT = Regex("^S-1-\\d+(?:-\\d+)+$")

    /**
     * SID of the account this process runs as. From the token, so it's the user's own SID even when
     * elevated (unlike a file owner). Uses `whoami`, GUI only.
     */
    fun currentUserSid(): String? = runCatching {
        val process =
            ProcessBuilder("whoami", "/user", "/fo", "csv", "/nh").redirectErrorStream(true).start()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        SID_PATTERN.find(process.inputStream.bufferedReader().readText())?.value
    }
        .onFailure { log.w(it) { "Failed to resolve current user SID" } }
        .getOrNull()

    /**
     * Reads a path's owning SID from the OS. The daemon installs the native backend's lookup at
     * startup. Left unset elsewhere, so [ownerOf] returns null.
     */
    @Volatile var ownerSidLookup: ((Path) -> String?)? = null

    /** [path]'s owning SID, or null if unknown or no lookup is installed. */
    fun ownerOf(path: Path): String? {
        val lookup = ownerSidLookup ?: return null
        return runCatching { lookup(path) }
            .onFailure { log.w(it) { "Failed to resolve owning SID of $path" } }
            .getOrNull()
            ?.takeIf { SID_EXACT.matches(it) }
    }
}

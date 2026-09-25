package com.zaneschepke.wireguardautotunnel.core.helper

import co.touchlab.kermit.Logger
import java.nio.file.Path
import java.util.Base64
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
     * SID of the account this process runs as. From the token, so it's the user's own SID even
     * when elevated (unlike a file owner). Uses `whoami`, not PowerShell. GUI only.
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

    /** [path]'s owning SID, or null if unknown (including owners without a fixed SID). */
    fun ownerOf(path: Path): String? = runCatching {
        // Path via environment variable + -EncodedCommand: as a -Command argument, powershell.exe
        // drops the quotes and splits "C:\Users\John Smith\x" at the space.
        // Owner via .Sddl, not .GetOwner(): Constrained Language Mode blocks method calls but not
        // property reads.
        val script = "(Get-Acl -LiteralPath \$env:WGT_SID_PATH).Sddl"
        val encoded =
            Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))

        val builder =
            ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded)
                // stderr not merged: PowerShell's error text must never be mistaken for a SID.
                .redirectError(ProcessBuilder.Redirect.DISCARD)
        builder.environment()["WGT_SID_PATH"] = path.toString()
        val process = builder.start()

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        val lastLine =
            process.inputStream.bufferedReader().readLines().lastOrNull { it.isNotBlank() }?.trim()
        lastLine?.let { parseSddlOwner(it) }
            ?: run {
                log.w { "SID lookup for $path returned no usable owner" }
                null
            }
    }
        .onFailure { log.w(it) { "Failed to resolve owning SID of $path" } }
        .getOrNull()

    /**
     * Owner of an SDDL string (`O:<owner>G:...`) as a SID. Aliases are language-independent and
     * mapped only when their SID is fixed; anything else is null.
     */
    internal fun parseSddlOwner(sddl: String): String? {
        val owner = SDDL_OWNER.find(sddl)?.groupValues?.get(1) ?: return null
        return if (SID_EXACT.matches(owner)) owner else SDDL_ALIASES[owner]
    }

    private val SDDL_OWNER = Regex("^O:(.+?)(?=G:|D:|S:|$)")

    private val SDDL_ALIASES =
        mapOf(
            "BA" to ADMINISTRATORS,
            "SY" to "S-1-5-18",
            "LS" to "S-1-5-19",
            "NS" to "S-1-5-20",
            "BU" to "S-1-5-32-545",
            "WD" to "S-1-1-0",
            "AU" to "S-1-5-11",
        )
}

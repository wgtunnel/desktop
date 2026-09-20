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

    /** Returns [path]'s owning SID or null if it can't be determined. */
    fun ownerOf(path: Path): String? = runCatching {
        // $p is bound as a parameter (rather than interpolated into the script text) so the
        // path itself doesn't need any PowerShell quoting/escaping.
        val process =
            ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "& {param(\$p) (Get-Acl -LiteralPath \$p)" +
                        ".GetOwner([System.Security.Principal.SecurityIdentifier]).Value}",
                    path.toString(),
                )
                .redirectErrorStream(true)
                .start()

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() }
    }
        .onFailure { log.w(it) { "Failed to resolve owning SID of $path" } }
        .getOrNull()
}

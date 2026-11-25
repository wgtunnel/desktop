package com.zaneschepke.wireguardautotunnel.daemon.util

import co.touchlab.kermit.Logger
import java.io.File
import java.util.concurrent.TimeUnit

/** Identifies the current boot, so state saved before a reboot is never reused after one. */
object BootId {
    private val log = Logger.withTag("BootId")
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    /** Null when it can't be read, which callers treat as a different boot. */
    val current: String? by lazy {
        runCatching {
            when {
                os.contains("win") -> windows()
                os.contains("mac") -> mac()
                else -> File("/proc/sys/kernel/random/boot_id").readText().trim()
            }
        }
            .onFailure { log.w(it) { "Failed to read the boot id" } }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    // "{ sec = 1700000000, usec = 123456 } Mon ..."
    private fun mac(): String? =
        Regex("sec = (\\d+), usec = (\\d+)").find(run("sysctl", "-n", "kern.boottime"))?.let {
            "${it.groupValues[1]}.${it.groupValues[2]}"
        }

    // Windows counts boots in BootId, a DWORD the kernel increments on every start.
    private fun windows(): String? =
        Regex("BootId\\s+REG_DWORD\\s+(0x[0-9a-fA-F]+)")
            .find(
                run(
                    "reg",
                    "query",
                    "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Memory Management\\PrefetchParameters",
                    "/v",
                    "BootId",
                ),
            )
            ?.groupValues
            ?.get(1)

    private fun run(vararg cmd: String): String {
        val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
        return out
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.service

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.system.Elevation
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import dev.nucleusframework.core.runtime.Platform
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Removes kill switch rules a stopped daemon left enforced, for when the daemon is not available.
 */
object KillSwitchUnblocker {
    private val log = Logger.withTag("KillSwitchUnblocker")

    enum class Outcome {
        REMOVED,
        CANCELLED,
        FAILED,
    }

    suspend fun removeBlock(): Outcome =
        withContext(Dispatchers.IO) {
            val daemon =
                daemonBinary()
                    ?: return@withContext Outcome.FAILED.also {
                        log.e { "Daemon binary not found next to the app" }
                    }
            runCatching { purge(daemon) }
                .onFailure { log.e(it) { "Failed to remove the kill switch block" } }
                .getOrDefault(Outcome.FAILED)
        }

    private fun purge(daemon: File): Outcome =
        when (Platform.Current) {
            Platform.Windows ->
                when (val result = Elevation.runAsAdmin(daemon.absolutePath, PURGE_ARG)) {
                    is Elevation.Result.Exited ->
                        if (result.code == 0) Outcome.REMOVED else Outcome.FAILED
                    Elevation.Result.Cancelled -> Outcome.CANCELLED
                    Elevation.Result.Failed -> Outcome.FAILED
                }
            Platform.MacOS ->
                exitOutcome(
                    run(
                        "osascript",
                        "-e",
                        "do shell script (quoted form of \"${appleScriptString(daemon.absolutePath)}\")" +
                            " & \" $PURGE_ARG\" with administrator privileges",
                    ),
                    cancelled = USER_CANCELED,
                )
            // pkexec exits 126 when the user dismisses the prompt.
            else -> exitOutcome(run("pkexec", daemon.absolutePath, PURGE_ARG), cancelled = 126)
        }

    private fun exitOutcome(code: Int, cancelled: Int): Outcome =
        when (code) {
            0 -> Outcome.REMOVED
            cancelled -> Outcome.CANCELLED
            else -> Outcome.FAILED
        }

    private fun run(vararg cmd: String): Int {
        val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            return -1
        }
        if (process.exitValue() != 0)
            log.w { "${cmd.first()} exited ${process.exitValue()}: $output" }
        // osascript exits 1 for a dismissed admin prompt as for any error, and prints -128.
        if (Platform.Current == Platform.MacOS && output.contains("(-128)")) return USER_CANCELED
        return process.exitValue()
    }

    // The daemon sits at bin/<name> next to the app binary on macOS and Linux, and next to it
    // on Windows.
    private fun daemonBinary(): File? {
        val appDir =
            ProcessHandle.current().info().command().orElse(null)?.let { File(it).parentFile }
                ?: return null
        val names = listOf("${AppVariant.current.linuxFsName}-daemon", "wgtunnel-daemon")
        val candidates =
            if (Platform.Current == Platform.Windows) names.map { File(appDir, "$it.exe") }
            else names.map { File(File(appDir, "bin"), it) }
        return candidates.firstOrNull { it.isFile }
    }

    private fun appleScriptString(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private const val PURGE_ARG = "--purge-firewall"
    private const val USER_CANCELED = -128
}

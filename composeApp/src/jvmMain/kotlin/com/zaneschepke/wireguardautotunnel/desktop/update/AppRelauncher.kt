package com.zaneschepke.wireguardautotunnel.desktop.update

import co.touchlab.kermit.Logger
import dev.nucleusframework.core.runtime.Platform
import java.io.File
import kotlin.system.exitProcess

/**
 * Restarts the app onto the binary now on disk, after a package manager replaced it while it ran.
 *
 * The new instance starts only after this one has exited. Until then it would find the
 * single-instance lock still held and hand off to this closing process instead.
 */
object AppRelauncher {
    private val log = Logger.withTag("AppRelauncher")

    fun relaunch() {
        val command =
            runCatching { launchCommand() }
                .onFailure { log.e(it) { "Cannot resolve the app to relaunch" } }
                .getOrNull() ?: return
        runCatching {
            ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
            .onSuccess { exitProcess(0) }
            .onFailure { log.e(it) { "Failed to start the relaunch helper" } }
    }

    private fun launchCommand(): List<String> {
        val executable = currentExecutable()
        val pid = ProcessHandle.current().pid().toString()
        return when (Platform.Current) {
            Platform.Windows ->
                // cmd has no wait-for-pid, and a hidden console can't use `timeout`.
                listOf(
                    "cmd.exe",
                    "/c",
                    "ping -n 3 127.0.0.1 >nul & start \"\" \"${executable.absolutePath}\"",
                )
            Platform.MacOS -> waitThen(pid, "open", appBundleOf(executable).absolutePath)
            else -> waitThen(pid, executable.absolutePath)
        }
    }

    private fun waitThen(pid: String, vararg launch: String): List<String> =
        listOf(
            "/bin/sh",
            "-c",
            "while kill -0 \"$1\" 2>/dev/null; do sleep 0.2; done; shift; exec \"$@\"",
            "relaunch",
            pid,
        ) + launch

    private fun currentExecutable(): File {
        // Linux reports a replaced binary as "<path> (deleted)", the path itself holds the new one.
        val path = ProcessHandle.current().info().command().orElseThrow().removeSuffix(" (deleted)")
        return File(path).also { check(it.isFile) { "Executable missing at $path" } }
    }

    private fun appBundleOf(executable: File): File =
        generateSequence(executable) { it.parentFile }.firstOrNull { it.name.endsWith(".app") }
            ?: error("No .app bundle above ${executable.absolutePath}")
}

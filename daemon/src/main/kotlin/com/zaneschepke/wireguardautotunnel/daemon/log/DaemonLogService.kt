package com.zaneschepke.wireguardautotunnel.daemon.log

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.core.log.LogRecorder
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.SharedFlow

class DaemonLogService(private val cacheRepository: DaemonCacheRepository) {
    private val log = Logger.withTag("DaemonLog")
    private val logDir = FilePathsHelper.getDaemonLogDir().toFile()
    private val recorder = LogRecorder(logDir = logDir, source = SOURCE)

    val messages: SharedFlow<LogMessageDto>
        get() = recorder.messages

    suspend fun restore() {
        if (cacheRepository.getLocalLoggingEnabled()) start()
    }

    suspend fun setEnabled(enabled: Boolean) {
        cacheRepository.setLocalLoggingEnabled(enabled)
        if (enabled) start() else stop()
    }

    suspend fun zipTo(destination: File) {
        recorder.zipTo(destination, extraFiles = nativeCaptureFiles())
    }

    suspend fun clear() {
        recorder.clear()
        nativeCaptureFiles().forEach { it.delete() }
    }

    private suspend fun start() {
        Files.createDirectories(logDir.toPath())
        PermissionsHelper.secureDaemonDataDirectory(logDir.toPath())
        recorder.start()
        log.i { "Local logging enabled at ${logDir.absolutePath}" }
    }

    private suspend fun stop() {
        recorder.stop()
        log.i { "Local logging disabled" }
    }

    /**
     * OS-captured native (Go) stderr. Windows WinSW already writes rotate files next to us. Linux
     * journal is exported on demand into a snapshot file so it can ride along in the zip.
     */
    private fun nativeCaptureFiles(): List<File> {
        val extras = mutableListOf<File>()
        val windows =
            System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        if (windows) {
            logDir
                .listFiles { f ->
                    f.isFile &&
                        !f.name.startsWith("log_") &&
                        (f.name.endsWith(".log") ||
                            f.name.endsWith(".out.log") ||
                            f.name.endsWith(".err.log") ||
                            f.name.endsWith(".wrapper.log"))
                }
                ?.let { extras.addAll(it) }
        } else {
            snapshotJournal()?.let { extras += it }
        }
        return extras
    }

    private fun snapshotJournal(): File? {
        val unit = "${AppVariant.current.linuxFsName}-daemon.service"
        return runCatching {
            val snapshot = File(logDir, "native-journal.txt")
            val process =
                ProcessBuilder(
                        "journalctl",
                        "-u",
                        unit,
                        "-n",
                        "2000",
                        "--no-pager",
                        "-o",
                        "short-iso",
                    )
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.readBytes()
            if (process.waitFor() == 0 && output.isNotEmpty()) {
                snapshot.writeBytes(output)
                snapshot
            } else null
        }
            .getOrNull()
    }

    companion object {
        const val SOURCE = "daemon"
    }
}

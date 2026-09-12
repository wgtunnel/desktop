package com.zaneschepke.wireguardautotunnel.daemon.log

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.helper.PermissionsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.core.log.LogRecorder
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.daemon.BuildConfig
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class DaemonLogService(
    private val cacheRepository: DaemonCacheRepository,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("DaemonLog")
    private val logDir = FilePathsHelper.getDaemonLogDir().toFile()
    private val recorder =
        LogRecorder(
            logDir = logDir,
            source = SOURCE,
            appVersionLabel = "${BuildConfig.APP_VERSION} (${AppVariant.current.id})",
        )
    private val nativeTailer = NativeLogTailer(logDir)
    @Volatile private var started = false

    // Native (Go) lines are live-view only, not persisted to our own files and handled by NativeLogTailer.
    val messages: Flow<LogMessageDto>
        get() = merge(recorder.messages, nativeTailer.messages)

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
        // The client asks us to enable logging both on its own settings flow subscription and
        // (separately) on every daemon reconnect, so this routinely fires twice in a row.
        if (started) return
        started = true
        withContext(Dispatchers.IO) {
            Files.createDirectories(logDir.toPath())
        }
        PermissionsHelper.secureDaemonDataDirectory(logDir.toPath())
        recorder.start()
        nativeTailer.start(scope)
        log.i { "Local logging enabled at ${logDir.absolutePath}" }
    }

    private suspend fun stop() {
        if (!started) return
        started = false
        nativeTailer.stop()
        recorder.stop()
        log.i { "Local logging disabled" }
    }

    /**
     * OS captured native go stderr. Windows WinSW already writes rotate files next to us. Linux
     * journal is exported on demand into a snapshot file so it can ride along in the zip.
     */
    private fun nativeCaptureFiles(): List<File> {
        val extras = mutableListOf<File>()
        val windows =
            System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        if (windows) {
            // Always <configBaseName>.{out,err,wrapper}.log - see WINSW_CONFIG_BASE_NAME.
            listOf("out", "err", "wrapper")
                .map { File(logDir, "$WINSW_CONFIG_BASE_NAME.$it.log") }
                .filter { it.isFile }
                .let { extras.addAll(it) }
        } else {
            snapshotJournal()?.let { extras += it }
        }
        return extras
    }

    // journalctl's text formats always prefix each line with the machine hostname.
    // -o json lets us pull just the timestamp and message and drop it.
    private fun snapshotJournal(): File? {
        val unit = "${AppVariant.current.linuxFsName}-daemon.service"
        return runCatching {
            val snapshot = File(logDir, "native-journal.txt")
            val process =
                ProcessBuilder("journalctl", "-u", unit, "-n", "2000", "--no-pager", "-o", "json")
                    .redirectErrorStream(true)
                    .start()
            val lines = process.inputStream.bufferedReader().readLines()
            if (process.waitFor() != 0 || lines.isEmpty()) return@runCatching null
            val text = lines.mapNotNull(::formatJournalLine).joinToString(System.lineSeparator())
            if (text.isBlank()) return@runCatching null
            snapshot.writeText(text + System.lineSeparator())
            snapshot
        }
            .getOrNull()
    }

    // Drop kermit's stdout to prevent duplication
    private fun formatJournalLine(rawLine: String): String? {
        val obj = runCatching { journalJson.parseToJsonElement(rawLine).jsonObject }.getOrNull() ?: return null
        val message = (obj["MESSAGE"] as? JsonPrimitive)?.contentOrNull ?: return null
        if (OWN_LOG_PREFIX.containsMatchIn(message)) return null
        val micros = (obj["__REALTIME_TIMESTAMP"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
        val timestamp =
            micros?.let { Instant.ofEpochSecond(it / 1_000_000, (it % 1_000_000) * 1_000) } ?: "?"
        return "$timestamp $message"
    }

    companion object {
        const val SOURCE = "daemon"
        // The literal filename StagePackagingSidecarsTask stages the WinSW config as - WinSW
        // derives its own log file base name from the config file's name.
        const val WINSW_CONFIG_BASE_NAME = "service-wrapper"
        private val journalJson = Json { ignoreUnknownKeys = true }
        private val OWN_LOG_PREFIX = Regex("^(Verbose|Debug|Info|Warn|Error|Assert): \\(")
    }
}

package com.zaneschepke.wireguardautotunnel.core.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LogRecorder(
    private val logDir: File,
    private val source: String,
    private val appVersionLabel: String = "unknown",
    private val maxFileSize: Long = MAX_FILE_SIZE,
    private val maxFolderSize: Long = MAX_FOLDER_SIZE,
) : LogWriter() {
    private val mutex = Mutex()
    private val writeLock = Any()
    @Volatile private var started = false
    private var currentFile: File? = null
    private var outputStream: FileOutputStream? = null

    private val _messages =
        MutableSharedFlow<LogMessageDto>(
            replay = REPLAY,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val messages: SharedFlow<LogMessageDto> = _messages.asSharedFlow()

    suspend fun start() {
        mutex.withLock {
            if (started) return
            logDir.mkdirs()
            Logger.setLogWriters(platformLogWriter(), this)
            started = true
        }
    }

    suspend fun stop() {
        mutex.withLock {
            if (!started) return
            Logger.setLogWriters(platformLogWriter())
            closeFile()
            started = false
        }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (!started) return
        val text =
            if (throwable != null) {
                buildString {
                    append(message)
                    append('\n')
                    append(throwable.stackTraceToString())
                }
            } else message
        val entry =
            LogMessageDto(
                time = Instant.now().toString(),
                level = severity.toSignifier(),
                tag = tag.ifBlank { source },
                message = text,
                source = source,
            )
        _messages.tryEmit(entry)
        synchronized(writeLock) { appendLine(entry.toString()) }
    }

    suspend fun zipTo(destination: File, extraFiles: List<File> = emptyList()) {
        mutex.withLock {
            closeFile()
            destination.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destination)).use { zos ->
                val files = (logDir.listFiles()?.toList().orEmpty() + extraFiles).distinct()
                files
                    .filter { it.isFile }
                    .forEach { file ->
                        zos.putNextEntry(ZipEntry("${source}/${file.name}"))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun clear() {
        mutex.withLock {
            closeFile()
            logDir.listFiles()?.forEach { it.delete() }
            _messages.resetReplayCache()
        }
    }

    private fun appendLine(line: String) {
        ensureFile()
        rotateIfNeeded()
        val stream = outputStream ?: return
        stream.write((line + System.lineSeparator()).toByteArray())
        stream.flush()
    }

    private fun ownedLogFiles(): Array<File>? =
        logDir.listFiles { f -> f.isFile && isOwnedFileName(f.name) }

    private fun isOwnedFileName(name: String): Boolean =
        name.startsWith(FILE_PREFIX) && name.endsWith("_$source.txt")

    private fun ensureFile() {
        if (currentFile != null && outputStream != null) return
        logDir.mkdirs()
        val latest = ownedLogFiles()?.maxByOrNull { it.lastModified() }
        if (latest != null && latest.length() < maxFileSize) {
            currentFile = latest
            outputStream = FileOutputStream(latest, true)
        } else {
            createNewFile()
        }
    }

    private fun createNewFile() {
        val stamp = LocalDateTime.now().format(FILE_TIME)
        val file = File(logDir, "${FILE_PREFIX}${stamp}_${source}.txt")
        currentFile = file
        outputStream = FileOutputStream(file)
        outputStream?.write(buildHeader().toByteArray())
        outputStream?.flush()
    }

    private fun buildHeader(): String = buildString {
        appendLine("=== WG Tunnel $source logs ===")
        appendLine("Version: $appVersionLabel")
        appendLine(
            "OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} " +
                "(${System.getProperty("os.arch")})"
        )
        appendLine("Started: ${LocalDateTime.now()}")
        appendLine("=========================")
        appendLine()
    }

    private fun rotateIfNeeded() {
        var folderSize = ownedFolderSize()
        while (folderSize >= maxFolderSize) {
            val victim =
                ownedLogFiles()
                    ?.filter { it != currentFile }
                    ?.minByOrNull { it.lastModified() } ?: break
            if (!victim.delete()) break
            folderSize = ownedFolderSize()
        }
        if ((currentFile?.length() ?: 0L) >= maxFileSize) {
            closeFile()
            createNewFile()
        }
    }

    private fun ownedFolderSize(): Long = ownedLogFiles()?.sumOf { it.length() } ?: 0L

    private fun closeFile() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null
        currentFile = null
    }

    companion object {
        const val MAX_FILE_SIZE = 2_097_152L
        const val MAX_FOLDER_SIZE = 10_485_760L
        private const val REPLAY = 10_000
        private const val FILE_PREFIX = "log_"
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        fun Severity.toSignifier(): String =
            when (this) {
                Severity.Verbose -> "V"
                Severity.Debug -> "D"
                Severity.Info -> "I"
                Severity.Warn -> "W"
                Severity.Error -> "E"
                Severity.Assert -> "A"
            }
    }
}

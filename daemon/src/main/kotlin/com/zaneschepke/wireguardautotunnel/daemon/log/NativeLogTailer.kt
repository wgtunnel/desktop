package com.zaneschepke.wireguardautotunnel.daemon.log

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import java.io.File
import java.io.RandomAccessFile
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class NativeLogTailer(private val logDir: File) {
    private val log = Logger.withTag("NativeLogTailer")
    private var job: Job? = null
    @Volatile private var process: Process? = null

    private val _messages =
        MutableSharedFlow<LogMessageDto>(
            replay = REPLAY,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val messages: SharedFlow<LogMessageDto> = _messages.asSharedFlow()

    fun start(scope: CoroutineScope) {
        stop()
        val windows =
            System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        job =
            scope.launch(Dispatchers.IO) {
                runCatching { if (windows) tailWindows() else tailLinux() }
                    .onFailure { log.w(it) { "Native log tail ended" } }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        process?.destroy()
        process = null
    }

    private fun tailLinux() {
        val unit = "${AppVariant.current.linuxFsName}-daemon.service"
        val proc =
            ProcessBuilder("journalctl", "-u", unit, "-f", "-n", "0", "--no-pager", "-o", "cat")
                .redirectErrorStream(true)
                .start()
        process = proc
        proc.inputStream.bufferedReader().useLines { lines -> lines.forEach(::forwardIfNative) }
    }

    private suspend fun tailWindows() {
        val file = findWinSwLogFile() ?: return
        var position = file.length()
        while (currentCoroutineContext().isActive) {
            val length = file.length()
            if (length < position) position = 0 // rotated
            if (length > position) {
                withContext(Dispatchers.IO) {
                    RandomAccessFile(file, "r").use { raf ->
                        raf.seek(position)
                        generateSequence { raf.readLine() }.forEach(::forwardIfNative)
                        position = raf.filePointer
                    }
                }
            }
            delay(POLL_INTERVAL_MS.milliseconds)
        }
    }

    // Native log lines land on stderr, which WinSW writes to its own .err file
    private fun findWinSwLogFile(): File? =
        logDir
            .listFiles { f ->
                f.isFile &&
                    !f.name.startsWith("log_") &&
                    (f.name.endsWith(".err") || f.name.endsWith(".err.log"))
            }
            ?.maxByOrNull { it.lastModified() }

    private fun forwardIfNative(line: String) {
        val (level, rest) =
            when {
                line.startsWith("[DEBUG] ") -> "D" to line.removePrefix("[DEBUG] ")
                line.startsWith("[ERROR] ") -> "E" to line.removePrefix("[ERROR] ")
                else -> return
            }
        val separator = rest.indexOf(": ")
        val tag = if (separator > 0) rest.substring(0, separator) else "Native"
        val message = if (separator > 0) rest.substring(separator + 2) else rest
        _messages.tryEmit(
            LogMessageDto(
                time = Instant.now().toString(),
                level = level,
                tag = tag,
                message = message,
                source = DaemonLogService.SOURCE,
            )
        )
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val REPLAY = 500
    }
}

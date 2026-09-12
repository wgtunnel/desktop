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
        // WinSW may not have created the .err file yet the moment we look so we loop
        // until we find it
        var file: File? = null
        var position = 0L
        while (currentCoroutineContext().isActive) {
            if (file?.exists() != true) {
                file = findWinSwLogFile()
                if (file == null) {
                    delay(POLL_INTERVAL_MS.milliseconds)
                    continue
                }
                position = file.length()
            }
            val target = file
            try {
                val length = target.length()
                if (length < position) position = 0 // rotated
                if (length > position) {
                    withContext(Dispatchers.IO) {
                        RandomAccessFile(target, "r").use { raf ->
                            raf.seek(position)
                            generateSequence { raf.readLine() }.forEach(::forwardIfNative)
                            position = raf.filePointer
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                log.d(e) { "Windows native log read failed, will retry" }
            }
            delay(POLL_INTERVAL_MS.milliseconds)
        }
    }

    private fun findWinSwLogFile(): File? =
        File(logDir, "${DaemonLogService.WINSW_CONFIG_BASE_NAME}.err.log").takeIf { it.isFile }

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

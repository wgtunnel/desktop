package com.zaneschepke.wireguardautotunnel.client.orchestration

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.core.log.LogRecorder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LogCoordinator(
    monitoringRepository: MonitoringSettingsRepository,
    private val daemonService: DaemonService,
    scope: CoroutineScope,
    appVersionLabel: String,
) {
    private val log = Logger.withTag("LogCoordinator")
    private val recorder =
        LogRecorder(
            logDir = FilePathsHelper.getAppLogDir(),
            source = SOURCE,
            appVersionLabel = appVersionLabel,
        )

    val messages: Flow<LogMessageDto> = merge(recorder.messages, daemonService.logsFlow().catch {})

    private val bufferLock = Any()
    private val logBuffer = ArrayDeque<LogMessageDto>()
    private val _bufferedMessages = MutableStateFlow<List<LogMessageDto>>(emptyList())
    val bufferedMessages: StateFlow<List<LogMessageDto>> = _bufferedMessages

    init {
        scope.launch {
            messages
                .onEach { message ->
                    synchronized(bufferLock) {
                        if (logBuffer.size >= MAX_BUFFER_SIZE) logBuffer.removeFirst()
                        logBuffer.addLast(message)
                    }
                }
                .sample(BATCH_INTERVAL)
                .collect { _bufferedMessages.value = synchronized(bufferLock) { logBuffer.toList() } }
        }
        scope.launch {
            monitoringRepository.flow
                .distinctUntilChangedBy { it.isLocalLogsEnabled }
                .collect { settings ->
                    if (settings.isLocalLogsEnabled) {
                        recorder.start()
                    } else {
                        recorder.stop()
                    }
                    syncDaemonLocalLogging(settings.isLocalLogsEnabled)
                }
        }
        // One-time sync for logger to keep daemon in sync
        scope.launch {
            daemonService.alive
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    val enabled = monitoringRepository.flow.first().isLocalLogsEnabled
                    syncDaemonLocalLogging(enabled)
                }
        }
    }

    private suspend fun syncDaemonLocalLogging(enabled: Boolean) {
        daemonService.setLocalLogging(enabled).onFailure {
            log.w(it) { "Failed to sync daemon local logging" }
        }
    }

    suspend fun clear() {
        recorder.clear()
        daemonService.clearLogs()
        synchronized(bufferLock) { logBuffer.clear() }
        _bufferedMessages.value = emptyList()
    }

    suspend fun exportZip(destination: File) {
        val appZip = File.createTempFile("wgtunnel-app-logs", ".zip")
        try {
            recorder.zipTo(appZip)
            val daemonBytes = daemonService.downloadLogZip().getOrNull()
            ZipOutputStream(FileOutputStream(destination)).use { out ->
                copyZipEntries(appZip, out)
                if (daemonBytes != null) {
                    ZipInputStream(daemonBytes.inputStream()).use { input ->
                        var entry = input.nextEntry
                        while (entry != null) {
                            out.putNextEntry(ZipEntry(entry.name))
                            input.copyTo(out)
                            out.closeEntry()
                            entry = input.nextEntry
                        }
                    }
                }
            }
        } finally {
            appZip.delete()
        }
    }

    private fun copyZipEntries(source: File, out: ZipOutputStream) {
        ZipInputStream(source.inputStream()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                out.putNextEntry(ZipEntry(entry.name))
                input.copyTo(out)
                out.closeEntry()
                entry = input.nextEntry
            }
        }
    }

    companion object {
        const val SOURCE = "app"
        const val MAX_BUFFER_SIZE = 10_000
        private val BATCH_INTERVAL = 200.milliseconds
    }
}

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class LogCoordinator(
    monitoringRepository: MonitoringSettingsRepository,
    private val daemonService: DaemonService,
    scope: CoroutineScope,
) {
    private val log = Logger.withTag("LogCoordinator")
    private val recorder = LogRecorder(logDir = FilePathsHelper.getAppLogDir(), source = SOURCE)

    val messages: Flow<LogMessageDto> = merge(recorder.messages, daemonService.logsFlow().catch {})

    init {
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
    }
}

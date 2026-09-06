package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.LoggerUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.FileUtils
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

@OptIn(FlowPreview::class)
class LoggerViewModel(private val logCoordinator: LogCoordinator) :
    OrbitContainerHost<LoggerUiState, LoggerUiState, AppSideEffect>, ViewModel() {

    private val logBuffer = ArrayDeque<LogMessageDto>()

    override val container =
        orbitContainer<LoggerUiState, AppSideEffect>(
            LoggerUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5_000L },
        ) {
            intent {
                logCoordinator.messages
                    .onEach { message ->
                        synchronized(logBuffer) {
                            if (logBuffer.size >= MAX_LOG_SIZE) logBuffer.removeFirst()
                            logBuffer.addLast(message)
                        }
                    }
                    .sample(BATCH_INTERVAL)
                    .collect {
                        val snapshot = synchronized(logBuffer) { logBuffer.toList() }
                        reduce { state.copy(messages = snapshot, isLoading = false) }
                    }
            }
            intent {
                delay(300.milliseconds)
                if (state.isLoading) reduce { state.copy(isLoading = false) }
            }
        }

    fun exportLogs() = intent {
        val stamp = FILE_TIME.format(Instant.now())
        val handle =
            FileKit.openFileSaver(
                suggestedName = "wgtunnel_logs_${stamp}_${BuildConfig.APP_VERSION}",
                defaultExtension = FileUtils.ZIP_FILE_EXTENSION,
                directory = null,
                dialogSettings = FileKitDialogSettings.createDefault(),
            )
        if (handle == null) {
            postSideEffect(AppSideEffect.Toast("Export cancelled", ToastType.Info))
            return@intent
        }
        val temp = java.io.File.createTempFile("wgtunnel-logs", ".zip")
        try {
            logCoordinator.exportZip(temp)
            handle.write(temp.readBytes())
            postSideEffect(
                AppSideEffect.Toast("Logs exported to ${handle.name}", ToastType.Success)
            )
        } catch (e: Exception) {
            postSideEffect(AppSideEffect.Toast("Export failed: ${e.message}", ToastType.Error))
        } finally {
            temp.delete()
        }
    }

    fun deleteLogs() = intent {
        synchronized(logBuffer) { logBuffer.clear() }
        reduce { state.copy(messages = emptyList()) }
        logCoordinator.clear()
        postSideEffect(AppSideEffect.Toast("Stored logs deleted", ToastType.Success))
    }

    companion object {
        const val MAX_LOG_SIZE = 10_000
        private val BATCH_INTERVAL = 200.milliseconds
        private val FILE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault())
    }
}

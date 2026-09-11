package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_cancelled
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.logs_exported_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stored_logs_deleted
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.unknown_error
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
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class LoggerViewModel(private val logCoordinator: LogCoordinator) :
    OrbitContainerHost<LoggerUiState, LoggerUiState, AppSideEffect>, ViewModel() {

    // Seeded synchronously from the coordinator's already buffered history (which survives
    // this ViewModel's own destruction/recreation across navigation)
    override val container =
        orbitContainer<LoggerUiState, AppSideEffect>(
            LoggerUiState(messages = logCoordinator.bufferedMessages.value, isLoading = false)
        ) {
            intent {
                logCoordinator.bufferedMessages.collect { snapshot ->
                    reduce { state.copy(messages = snapshot, isLoading = false) }
                }
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
            postSideEffect(
                AppSideEffect.Toast(getString(Res.string.export_cancelled), ToastType.Info)
            )
            return@intent
        }
        val temp = java.io.File.createTempFile("wgtunnel-logs", ".zip")
        try {
            logCoordinator.exportZip(temp)
            handle.write(temp.readBytes())
            postSideEffect(
                AppSideEffect.Toast(
                    getString(Res.string.logs_exported_template, handle.name),
                    ToastType.Success,
                )
            )
        } catch (e: Exception) {
            postSideEffect(
                AppSideEffect.Toast(
                    getString(
                        Res.string.export_failed,
                        e.message ?: getString(Res.string.unknown_error),
                    ),
                    ToastType.Error,
                )
            )
        } finally {
            temp.delete()
        }
    }

    fun deleteLogs() = intent {
        logCoordinator.clear()
        postSideEffect(
            AppSideEffect.Toast(getString(Res.string.stored_logs_deleted), ToastType.Success)
        )
    }

    companion object {
        private val FILE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault())
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.update

import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_check_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_download_failed
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import dev.nucleusframework.core.runtime.ExecutableRuntime
import dev.nucleusframework.updater.DownloadProgress
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.UpdateFile
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateLevel
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.provider.GitHubProvider
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import org.jetbrains.compose.resources.getString

class AppUpdater(
    private val updater: NucleusUpdater = NucleusUpdater {
        currentVersion = BuildConfig.APP_VERSION
        provider = GitHubProvider(owner = "wgtunnel", repo = "desktop")
        channel = AppVariant.current.updateChannel
        allowPrerelease = AppVariant.current == AppVariant.BETA
    }
) {
    // Test hook for testing the updater flow
    private val simulateUpdate: Boolean =
        AppVariant.current == AppVariant.DEBUG &&
            System.getenv("WGTUNNEL_DEBUG_FAKE_UPDATE").toBoolean()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun isSupported(): Boolean =
        simulateUpdate || (updater.isUpdateSupported() && !ExecutableRuntime.isPacman())

    // silent is for the automatic background check at app startup
    suspend fun check(silent: Boolean = false): UpdateResult {
        if (!isSupported()) return UpdateResult.NotAvailable
        _state.value = UpdateState.Checking
        val result = if (simulateUpdate) fakeAvailableResult() else updater.checkForUpdates()
        _state.value =
            when (result) {
                is UpdateResult.Available -> UpdateState.Available(result.info, result.level)
                UpdateResult.NotAvailable -> UpdateState.Idle
                is UpdateResult.Error ->
                    if (silent) UpdateState.Idle
                    else
                        UpdateState.Failed(
                            result.exception.message ?: getString(Res.string.update_check_failed)
                        )
            }
        return result
    }

    suspend fun download(info: UpdateInfo) {
        _state.value = UpdateState.Downloading(0L, info.currentFile.size, 0.0, false)
        var lastEmitMs = 0L
        runCatching {
            (if (simulateUpdate) fakeDownloadFlow() else updater.downloadUpdate(info)).collect {
                progress ->
                val file = progress.file
                if (file != null) {
                    _state.value = UpdateState.ReadyToInstall(info.version, file)
                    return@collect
                }
                val now = System.currentTimeMillis()
                if (now - lastEmitMs < PROGRESS_SAMPLE_MS) return@collect
                lastEmitMs = now
                _state.value =
                    UpdateState.Downloading(
                        progress.bytesDownloaded,
                        progress.totalBytes,
                        progress.percent,
                        progress.isDifferential,
                    )
            }
        }
            .onFailure {
                _state.value = UpdateState.Failed(getString(Res.string.update_download_failed))
            }
    }

    fun install(file: File) {
        _state.value = UpdateState.Installing
        if (simulateUpdate) return
        updater.installAndRestart(file)
    }

    private fun fakeAvailableResult(): UpdateResult.Available {
        val file =
            UpdateFile(
                url = "",
                sha512 = "",
                size = FAKE_TOTAL_BYTES,
                fileName = "wgtunnel-fake-update",
            )
        return UpdateResult.Available(
            info =
                UpdateInfo(
                    version = "99.9.9",
                    releaseDate = "1970-01-01",
                    files = listOf(file),
                    currentFile = file,
                ),
            level = UpdateLevel.MINOR,
        )
    }

    private fun fakeDownloadFlow(): Flow<DownloadProgress> = flow {
        var downloaded = 0L
        val step = FAKE_TOTAL_BYTES / FAKE_STEP_COUNT
        while (downloaded < FAKE_TOTAL_BYTES) {
            delay(FAKE_STEP_DELAY_MS.milliseconds)
            downloaded = (downloaded + step).coerceAtMost(FAKE_TOTAL_BYTES)
            emit(
                DownloadProgress(
                    bytesDownloaded = downloaded,
                    totalBytes = FAKE_TOTAL_BYTES,
                    percent = downloaded.toDouble() / FAKE_TOTAL_BYTES * 100.0,
                    isDifferential = true,
                )
            )
        }
        delay(FAKE_STEP_DELAY_MS.milliseconds)
        emit(
            DownloadProgress(
                bytesDownloaded = FAKE_TOTAL_BYTES,
                totalBytes = FAKE_TOTAL_BYTES,
                percent = 100.0,
                file = File.createTempFile("wgtunnel-fake-update", ".tmp"),
                isDifferential = true,
            )
        )
    }

    private companion object {
        const val FAKE_TOTAL_BYTES = 42_000_000L
        const val FAKE_STEP_COUNT = 20L
        const val FAKE_STEP_DELAY_MS = 150L
        const val PROGRESS_SAMPLE_MS = 150L
    }
}

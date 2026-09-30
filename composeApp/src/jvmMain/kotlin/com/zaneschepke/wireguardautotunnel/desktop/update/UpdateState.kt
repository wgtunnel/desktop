package com.zaneschepke.wireguardautotunnel.desktop.update

import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateLevel
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class Available(val info: UpdateInfo, val level: UpdateLevel) : UpdateState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percent: Double,
        val isDifferential: Boolean,
    ) : UpdateState

    data class ReadyToInstall(val version: String, val file: File) : UpdateState

    data object Installing : UpdateState

    data class Failed(val message: String) : UpdateState
}

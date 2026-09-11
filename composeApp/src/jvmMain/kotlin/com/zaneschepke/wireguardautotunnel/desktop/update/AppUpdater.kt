package com.zaneschepke.wireguardautotunnel.desktop.update

import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import dev.nucleusframework.core.runtime.ExecutableRuntime
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.provider.GitHubProvider
import kotlinx.coroutines.flow.last

class AppUpdater(
    private val updater: NucleusUpdater = NucleusUpdater {
        currentVersion = BuildConfig.APP_VERSION
        provider = GitHubProvider(owner = "wgtunnel", repo = "desktop")
        channel = AppVariant.current.updateChannel
        allowPrerelease = AppVariant.current == AppVariant.BETA
    }
) {
    fun isSupported(): Boolean = updater.isUpdateSupported() && !ExecutableRuntime.isPacman()

    suspend fun check(): UpdateResult {
        if (!isSupported()) return UpdateResult.NotAvailable
        return updater.checkForUpdates()
    }

    suspend fun downloadAndInstall(info: UpdateInfo) {
        val progress = updater.downloadUpdate(info).last()
        val file = progress.file ?: error("Update download finished without a file")
        updater.installAndRestart(file)
    }
}

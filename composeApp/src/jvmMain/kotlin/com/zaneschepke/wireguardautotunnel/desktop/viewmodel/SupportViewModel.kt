package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.up_to_date
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_check_failed
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SupportUiState
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import com.zaneschepke.wireguardautotunnel.desktop.update.UpdateState
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateResult
import java.io.File
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class SupportViewModel(private val appUpdater: AppUpdater) :
    OrbitContainerHost<SupportUiState, SupportUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<SupportUiState, AppSideEffect>(
            SupportUiState(
                updateState = appUpdater.state.value,
                updateSupported = appUpdater.isSupported(),
            )
        ) {
            intent {
                appUpdater.state.collect { updateState ->
                    reduce { state.copy(updateState = updateState) }
                }
            }
        }

    fun onCheckForUpdate() = intent {
        if (!state.updateSupported) return@intent
        val current = state.updateState
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return@intent
        when (val result = appUpdater.check()) {
            is UpdateResult.Available -> Unit // appUpdater.state already reflects it
            is UpdateResult.NotAvailable -> {
                postSideEffect(
                    AppSideEffect.Toast(getString(Res.string.up_to_date), ToastType.Success)
                )
            }
            is UpdateResult.Error -> {
                val message = result.exception.message ?: getString(Res.string.update_check_failed)
                postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
            }
        }
    }

    fun onDownloadUpdate(info: UpdateInfo) = intent { appUpdater.download(info) }

    fun onInstallUpdate(file: File) = intent { appUpdater.install(file) }
}

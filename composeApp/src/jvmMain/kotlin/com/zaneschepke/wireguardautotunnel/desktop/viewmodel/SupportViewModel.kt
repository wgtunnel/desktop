package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SupportUiState
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateResult
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class SupportViewModel(private val appUpdater: AppUpdater) :
    OrbitContainerHost<SupportUiState, SupportUiState, AppSideEffect>, ViewModel() {

    private var pendingUpdate: UpdateInfo? = null

    override val container = orbitContainer<SupportUiState, AppSideEffect>(SupportUiState()) {}

    fun onUpdateAction() = intent {
        if (state.updateBusy) return@intent
        val pending = pendingUpdate
        if (pending != null) {
            reduce { state.copy(updateBusy = true, updateMessage = null) }
            runCatching { appUpdater.downloadAndInstall(pending) }
                .onFailure { error ->
                    reduce { state.copy(updateBusy = false, updateMessage = error.message) }
                    postSideEffect(
                        AppSideEffect.Toast(error.message ?: "Update failed", ToastType.Error)
                    )
                }
            return@intent
        }
        reduce { state.copy(updateBusy = true, updateMessage = null) }
        when (val result = appUpdater.check()) {
            is UpdateResult.Available -> {
                pendingUpdate = result.info
                reduce {
                    state.copy(
                        updateBusy = false,
                        pendingUpdateVersion = result.info.version,
                        updateMessage = result.info.version,
                    )
                }
            }
            is UpdateResult.NotAvailable -> {
                pendingUpdate = null
                reduce {
                    state.copy(
                        updateBusy = false,
                        pendingUpdateVersion = null,
                        updateSupported = appUpdater.isSupported(),
                        alreadyLatest = true,
                    )
                }
            }
            is UpdateResult.Error -> {
                pendingUpdate = null
                val message = result.exception.message ?: "Update check failed"
                reduce { state.copy(updateBusy = false, updateMessage = message) }
                postSideEffect(AppSideEffect.Toast(message, ToastType.Error))
            }
        }
    }
}

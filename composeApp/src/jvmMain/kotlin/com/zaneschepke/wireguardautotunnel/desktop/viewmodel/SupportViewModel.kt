package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.up_to_date
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_check_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_download_failed
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.SupportUiState
import com.zaneschepke.wireguardautotunnel.desktop.update.AppUpdater
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateResult
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class SupportViewModel(private val appUpdater: AppUpdater) :
    OrbitContainerHost<SupportUiState, SupportUiState, AppSideEffect>, ViewModel() {

    private var pendingUpdate: UpdateInfo? = null

    override val container =
        orbitContainer<SupportUiState, AppSideEffect>(
            SupportUiState(updateSupported = appUpdater.isSupported())
        ) {}

    fun onUpdateAction() = intent {
        if (state.updateBusy || !state.updateSupported) return@intent
        val pending = pendingUpdate
        if (pending != null) {
            reduce { state.copy(updateBusy = true) }
            runCatching { appUpdater.downloadAndInstall(pending) }
                .onFailure { error ->
                    reduce { state.copy(updateBusy = false) }
                    postSideEffect(
                        AppSideEffect.Toast(
                            error.message ?: getString(Res.string.update_download_failed),
                            ToastType.Error,
                        )
                    )
                }
            return@intent
        }
        reduce { state.copy(updateBusy = true) }
        when (val result = appUpdater.check()) {
            is UpdateResult.Available -> {
                pendingUpdate = result.info
                reduce {
                    state.copy(updateBusy = false, pendingUpdateVersion = result.info.version)
                }
            }
            is UpdateResult.NotAvailable -> {
                pendingUpdate = null
                reduce { state.copy(updateBusy = false, pendingUpdateVersion = null) }
                postSideEffect(
                    AppSideEffect.Toast(getString(Res.string.up_to_date), ToastType.Success)
                )
            }
            is UpdateResult.Error -> {
                pendingUpdate = null
                reduce { state.copy(updateBusy = false) }
                postSideEffect(
                    AppSideEffect.Toast(
                        result.exception.message ?: getString(Res.string.update_check_failed),
                        ToastType.Error,
                    )
                )
            }
        }
    }
}

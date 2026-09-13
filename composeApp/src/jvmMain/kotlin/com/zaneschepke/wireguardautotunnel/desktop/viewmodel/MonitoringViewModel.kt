package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.enums.StatisticRefresh
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.refresh_rate_updated
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.MonitoringUiState
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class MonitoringViewModel(private val monitoringSettingsRepository: MonitoringSettingsRepository) :
    OrbitContainerHost<MonitoringUiState, MonitoringUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<MonitoringUiState, AppSideEffect>(MonitoringUiState()) {
            intent {
                monitoringSettingsRepository.flow.collect { settings ->
                    reduce {
                        state.copy(
                            isLoaded = true,
                            tunnelStatisticsEnabled = settings.tunnelStatisticsEnabled,
                            statisticRefresh =
                                StatisticRefresh.fromValue(settings.tunnelStatisticsPollInterval),
                        )
                    }
                }
            }
        }

    fun onLiveTunnelStatisticsChanged(enabled: Boolean) = intent {
        val current = monitoringSettingsRepository.get()
        monitoringSettingsRepository.upsert(current.copy(tunnelStatisticsEnabled = enabled))
        reduce { state.copy(tunnelStatisticsEnabled = enabled) }
    }

    fun onStatisticsIntervalChanged(refresh: StatisticRefresh) = intent {
        val current = monitoringSettingsRepository.get()
        monitoringSettingsRepository.upsert(
            current.copy(tunnelStatisticsPollInterval = refresh.value)
        )
        reduce { state.copy(statisticRefresh = refresh) }
        postSideEffect(
            AppSideEffect.Toast(getString(Res.string.refresh_rate_updated), ToastType.Success)
        )
    }
}

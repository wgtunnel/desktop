package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bssid_pattern_in_use
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.network_pattern_in_use
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AutoTunnelUiState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class AutoTunnelViewModel(
    private val autoTunnelRepository: AutoTunnelSettingsRepository,
    private val tunnelRepository: TunnelRepository,
    private val daemonService: DaemonService,
) : OrbitContainerHost<AutoTunnelUiState, AutoTunnelUiState, AppSideEffect>, ViewModel() {

    val ssidHints: List<String>
        get() =
            if (container.stateFlow.value.autoTunnelSettings.isWildcardsEnabled) {
                listOf("Office_WiFi", "Home*", "*Guest*", "!Hotel_WiFi", "Cafe?")
            } else {
                listOf("Home_WiFi")
            }

    val bssidHints: List<String>
        get() =
            if (container.stateFlow.value.autoTunnelSettings.isWildcardsEnabled) {
                listOf("AA:BB:CC:DD:EE:FF", "AA:BB:CC:*", "!AA:BB:CC:DD:EE:FF")
            } else {
                listOf("AA:BB:CC:DD:EE:FF")
            }

    override val container =
        orbitContainer<AutoTunnelUiState, AppSideEffect>(AutoTunnelUiState()) {
            intent {
                combine(
                        autoTunnelRepository.flow,
                        tunnelRepository.userTunnelsFlow,
                        daemonService.autoTunnelStatusFlow().onStart {
                            emit(AutoTunnelStatusDto())
                        },
                    ) { settings, tunnels, status ->
                        Triple(settings, tunnels, status)
                    }
                    .collect { (settings, tunnels, status) ->
                        reduce {
                            state.copy(
                                isLoaded = true,
                                autoTunnelSettings = settings,
                                tunnels = tunnels,
                                network = status.network,
                                autoTunnelActive = settings.isAutoTunnelEnabled,
                            )
                        }
                    }
            }
            intent { daemonService.alive.collect { reduce { state.copy(daemonConnected = it) } } }
        }

    fun toggleAutoTunnel() = intent {
        if (!state.daemonConnected) return@intent
        autoTunnelRepository.updateAutoTunnelEnabled(!state.autoTunnelSettings.isAutoTunnelEnabled)
    }

    fun setTunnelOnWifi(enabled: Boolean) = intent {
        autoTunnelRepository.upsert(state.autoTunnelSettings.copy(isTunnelOnWifiEnabled = enabled))
    }

    fun setTunnelOnEthernet(enabled: Boolean) = intent {
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(isTunnelOnEthernetEnabled = enabled)
        )
    }

    fun setStopOnNoInternet(enabled: Boolean) = intent {
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(isStopOnNoInternetEnabled = enabled)
        )
    }

    fun setStartOnBoot(enabled: Boolean) = intent {
        if (!state.daemonConnected) return@intent
        autoTunnelRepository.upsert(state.autoTunnelSettings.copy(startOnBoot = enabled))
    }

    fun setWildcardsEnabled(enabled: Boolean) = intent {
        autoTunnelRepository.upsert(state.autoTunnelSettings.copy(isWildcardsEnabled = enabled))
    }

    fun saveTrustedNetworkName(name: String) = intent {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@intent
        if (state.autoTunnelSettings.trustedNetworkSsids.contains(trimmed)) {
            postSideEffect(
                AppSideEffect.Toast(
                    getString(Res.string.network_pattern_in_use),
                    ToastType.Error,
                )
            )
            return@intent
        }
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(
                trustedNetworkSsids = state.autoTunnelSettings.trustedNetworkSsids + trimmed
            )
        )
    }

    fun removeTrustedNetworkName(name: String) = intent {
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(
                trustedNetworkSsids = state.autoTunnelSettings.trustedNetworkSsids - name
            )
        )
    }

    fun saveTrustedBssid(bssid: String) = intent {
        val trimmed = bssid.trim().uppercase()
        if (trimmed.isEmpty()) return@intent
        if (state.autoTunnelSettings.trustedNetworkBssids.contains(trimmed)) {
            postSideEffect(
                AppSideEffect.Toast(getString(Res.string.bssid_pattern_in_use), ToastType.Error)
            )
            return@intent
        }
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(
                trustedNetworkBssids = state.autoTunnelSettings.trustedNetworkBssids + trimmed
            )
        )
    }

    fun removeTrustedBssid(bssid: String) = intent {
        autoTunnelRepository.upsert(
            state.autoTunnelSettings.copy(
                trustedNetworkBssids = state.autoTunnelSettings.trustedNetworkBssids - bssid
            )
        )
    }

    fun setEthernetTunnel(tunnel: TunnelConfig?) = intent {
        tunnelRepository.updateEthernetTunnel(tunnel)
    }

    fun saveWifiMapping(tunnel: TunnelConfig, ssid: String) = intent {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty()) return@intent
        if (tunnel.tunnelNetworks.contains(trimmed)) {
            postSideEffect(
                AppSideEffect.Toast(
                    getString(Res.string.network_pattern_in_use),
                    ToastType.Error,
                )
            )
            return@intent
        }
        tunnelRepository.save(tunnel.copy(tunnelNetworks = tunnel.tunnelNetworks + trimmed))
    }

    fun removeWifiMapping(tunnel: TunnelConfig, ssid: String) = intent {
        tunnelRepository.save(tunnel.copy(tunnelNetworks = tunnel.tunnelNetworks - ssid))
    }

    fun saveBssidMapping(tunnel: TunnelConfig, bssid: String) = intent {
        val trimmed = bssid.trim().uppercase()
        if (trimmed.isEmpty()) return@intent
        if (tunnel.tunnelBssids.contains(trimmed)) {
            postSideEffect(
                AppSideEffect.Toast(getString(Res.string.bssid_pattern_in_use), ToastType.Error)
            )
            return@intent
        }
        tunnelRepository.save(tunnel.copy(tunnelBssids = tunnel.tunnelBssids + trimmed))
    }

    fun removeBssidMapping(tunnel: TunnelConfig, bssid: String) = intent {
        tunnelRepository.save(tunnel.copy(tunnelBssids = tunnel.tunnelBssids - bssid))
    }
}

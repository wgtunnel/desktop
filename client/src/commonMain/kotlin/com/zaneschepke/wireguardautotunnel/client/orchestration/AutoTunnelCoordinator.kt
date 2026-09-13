package com.zaneschepke.wireguardautotunnel.client.orchestration

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.model.AutoTunnelSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelSettingsDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelTunnelConfigDto
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoTunnelCoordinator(
    private val daemonService: DaemonService,
    private val tunnelCoordinator: TunnelCoordinator,
    autoTunnelRepository: AutoTunnelSettingsRepository,
    tunnelRepository: TunnelRepository,
    settingsRepository: GeneralSettingRepository,
    dnsSettingsRepository: DnsSettingsRepository,
    monitoringSettingsRepository: MonitoringSettingsRepository,
    proxyRepository: ProxySettingsRepository,
    lockdownRepository: LockdownSettingsRepository,
    scope: CoroutineScope,
) {
    private val log = Logger.withTag("AutoTunnelCoordinator")
    private val configToUpdate = MutableStateFlow<AutoTunnelConfigDto?>(null)
    private var lastPushed: AutoTunnelConfigDto? = null

    private val runtimeSettings =
        combine(
            settingsRepository.flow,
            dnsSettingsRepository.flow,
            monitoringSettingsRepository.flow,
            proxyRepository.flow,
            lockdownRepository.flow,
        ) { general, dns, monitoring, proxy, lockdown ->
            TunnelCoordinator.RuntimeSettingsSnapshot(general, dns, monitoring, proxy, lockdown)
        }

    init {
        @OptIn(FlowPreview::class)
        scope.launch {
            combine(
                    autoTunnelRepository.flow,
                    tunnelRepository.userTunnelsFlow,
                    runtimeSettings,
                ) { settings, tunnels, snapshot ->
                    Triple(settings, tunnels, snapshot)
                }
                .distinctUntilChanged()
                .debounce(300.milliseconds)
                .collectLatest { (settings, tunnels, _) ->
                    configToUpdate.value = buildConfig(settings, tunnels)
                }
        }
        scope.launch {
            combine(configToUpdate.filterNotNull(), daemonService.alive) { plan, alive ->
                    plan to alive
                }
                .collectLatest { (plan, alive) ->
                    if (!alive) {
                        lastPushed = null
                        return@collectLatest
                    }
                    if (plan == lastPushed) return@collectLatest
                    if (updateConfig(plan)) lastPushed = plan
                }
        }
    }

    private suspend fun buildConfig(
        settings: AutoTunnelSettings,
        tunnels: List<TunnelConfig>,
    ): AutoTunnelConfigDto {
        val autoTunnelTunnelConfigs = tunnels.mapNotNull { config ->
            runCatching {
                AutoTunnelTunnelConfigDto(
                    id = config.id,
                    name = config.name,
                    isPrimaryTunnel = config.isPrimaryTunnel,
                    isEthernetTunnel = config.isEthernetTunnel,
                    tunnelNetworks = config.tunnelNetworks,
                    tunnelBssids = config.tunnelBssids,
                    startRequest = tunnelCoordinator.prepareStartRequest(config),
                )
            }
                .onFailure { log.e(it) { "Failed to build auto-tunnel plan for ${config.name}" } }
                .getOrNull()
        }
        return AutoTunnelConfigDto(
            enabled = settings.isAutoTunnelEnabled,
            startOnBoot = settings.startOnBoot,
            settings =
                AutoTunnelSettingsDto(
                    isTunnelOnWifiEnabled = settings.isTunnelOnWifiEnabled,
                    isTunnelOnEthernetEnabled = settings.isTunnelOnEthernetEnabled,
                    isWildcardsEnabled = settings.isWildcardsEnabled,
                    isStopOnNoInternetEnabled = settings.isStopOnNoInternetEnabled,
                    trustedNetworkSsids = settings.trustedNetworkSsids,
                    trustedNetworkBssids = settings.trustedNetworkBssids,
                ),
            tunnels = autoTunnelTunnelConfigs,
        )
    }

    private suspend fun updateConfig(config: AutoTunnelConfigDto): Boolean {
        while (currentCoroutineContext().isActive) {
            val result = daemonService.updateAutoTunnelConfig(config)
            if (result.isSuccess) {
                log.d {
                    "Updated auto-tunnel config enabled=${config.enabled} tunnels=${config.tunnels.size}"
                }
                return true
            }
            if (!daemonService.alive()) {
                log.w(result.exceptionOrNull()) {
                    "Failed to update auto-tunnel config, daemon is not healthy"
                }
                return false
            }
            log.w(result.exceptionOrNull()) { "Failed to push auto-tunnel config, retrying..." }
            delay(2.seconds)
        }
        return false
    }
}

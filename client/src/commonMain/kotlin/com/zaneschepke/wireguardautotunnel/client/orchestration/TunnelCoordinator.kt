package com.zaneschepke.wireguardautotunnel.client.orchestration

import com.wgtunnel.backend.util.parseDnsServersOnly
import com.wgtunnel.parser.AmneziaConfigNormalizer
import com.wgtunnel.parser.Config
import com.wgtunnel.parser.ConfigReconciler
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.GeneralSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.MonitoringSettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.MonitoringSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelService
import com.zaneschepke.wireguardautotunnel.client.util.toDto
import com.zaneschepke.wireguardautotunnel.client.util.toTunnelDnsConfigOrNull
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelFeaturesDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelModeDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TunnelCoordinator(
    private val tunnelService: TunnelService,
    private val backendService: BackendService,
    private val tunnelRepository: TunnelRepository,
    settingsRepository: GeneralSettingRepository,
    dnsSettingsRepository: DnsSettingsRepository,
    monitoringSettingsRepository: MonitoringSettingsRepository,
    proxyRepository: ProxySettingsRepository,
    lockdownRepository: LockdownSettingsRepository,
    scope: CoroutineScope,
) {
    data class RuntimeSettingsSnapshot(
        val general: GeneralSettings,
        val dns: DnsSettings,
        val monitoring: MonitoringSettings,
        val proxy: ProxySettings,
        val lockdown: LockdownSettings,
    )

    private val runtimeSettingsSnapshot =
        combine(
            settingsRepository.flow,
            dnsSettingsRepository.flow,
            monitoringSettingsRepository.flow,
            proxyRepository.flow,
            lockdownRepository.flow,
        ) { general, dns, monitoring, proxy, lockdown ->
            RuntimeSettingsSnapshot(general, dns, monitoring, proxy, lockdown)
        }

    private val runtimeSettingsSnapshotState =
        runtimeSettingsSnapshot.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private val tunnelMutex = Mutex()

    private suspend fun getSnapshot(): RuntimeSettingsSnapshot {
        return runtimeSettingsSnapshotState.filterNotNull().first()
    }

    suspend fun activeTunnelIds(): List<Long> {
        return backendService.getStatus().getOrNull()?.activeTunnels?.map { it.id }.orEmpty()
    }

    suspend fun startTunnel(config: TunnelConfig): Result<Unit> = tunnelMutex.withLock {
        activeTunnelIds().filter { it != config.id }.forEach { tunnelService.stopTunnel(it) }
        val snapshot = getSnapshot()
        val request = buildStartRequest(config, snapshot)
        tunnelService.startTunnel(config.id, request)
    }

    suspend fun stopTunnel(id: Long): Result<Unit> = tunnelMutex.withLock {
        tunnelService.stopTunnel(id)
    }

    suspend fun stopActiveTunnels(): Result<Unit> = tunnelMutex.withLock {
        activeTunnelIds().forEach { tunnelService.stopTunnel(it) }
        Result.success(Unit)
    }

    suspend fun restartActiveTunnels(): Result<Unit> {
        val activeIds = activeTunnelIds()
        if (activeIds.isEmpty()) return Result.success(Unit)
        val configs = activeIds.mapNotNull { tunnelRepository.getById(it) }
        stopActiveTunnels().getOrElse {
            return Result.failure(it)
        }
        configs.forEach {
            startTunnel(it).getOrElse { error ->
                return Result.failure(error)
            }
        }
        return Result.success(Unit)
    }

    suspend fun toggleTunnel(config: TunnelConfig): Result<Unit> {
        val isActive = activeTunnelIds().contains(config.id)
        return if (isActive) stopTunnel(config.id) else startTunnel(config)
    }

    fun killSwitchDto(lockdown: LockdownSettings, runConfig: Config?): KillSwitchConfigDto {
        val dnsServers = runConfig?.parseDnsServersOnly().orEmpty()
        return lockdown.toDto(dnsServers)
    }

    suspend fun prepareStartRequest(tunnelConfig: TunnelConfig): StartTunnelRequest {
        return buildStartRequest(tunnelConfig, getSnapshot())
    }

    private suspend fun buildStartRequest(
        tunnelConfig: TunnelConfig,
        snapshot: RuntimeSettingsSnapshot,
    ): StartTunnelRequest {
        var config = AmneziaConfigNormalizer.ensureAmneziaCompatibility(tunnelConfig.asConfig())
        val policy =
            ConfigReconciler.ConfigReconcilePolicy(
                dns = snapshot.dns.isGlobalTunnelConfigDnsEnabled,
                splitTunnel = false,
                amnezia = snapshot.general.isGlobalAmneziaEnabled,
            )
        if (policy.hasAnyOverrides) {
            val globalConfig = tunnelRepository.globalTunnelFlow.firstOrNull()?.asConfig()
            config = ConfigReconciler.reconcileConfig(config, globalConfig, policy)
        }

        val dns = snapshot.dns.toTunnelDnsConfigOrNull(config)
        val uiMode = snapshot.general.selectableTunnelMode
        val killSwitchEnabled = snapshot.lockdown.enabled
        val mode =
            when {
                uiMode == TunnelMode.PROXY && killSwitchEnabled -> TunnelModeDto.LOCK_DOWN
                uiMode == TunnelMode.PROXY -> TunnelModeDto.PROXY
                else -> TunnelModeDto.VPN
            }

        return StartTunnelRequest(
            name = tunnelConfig.name,
            quickConfig = config.asQuickString(),
            mode = mode,
            tunnelDns = dns?.toDto(),
            proxy = if (uiMode == TunnelMode.PROXY) snapshot.proxy.toDto() else null,
            killSwitch = if (killSwitchEnabled) killSwitchDto(snapshot.lockdown, config) else null,
            features =
                TunnelFeaturesDto(
                    statisticsEnabled = snapshot.monitoring.tunnelStatisticsEnabled,
                    statisticsPollIntervalSeconds =
                        snapshot.monitoring.tunnelStatisticsPollInterval,
                    seamlessRecoveryEnabled = snapshot.general.seamlessRecoveryEnabled,
                    dynamicDnsRecovery = tunnelConfig.isDdnsTunnel,
                ),
            preferIpv6 = tunnelConfig.preferIpv6,
            ipv6RestoreEnabled = tunnelConfig.ipv6RestoreEnabled,
        )
    }
}

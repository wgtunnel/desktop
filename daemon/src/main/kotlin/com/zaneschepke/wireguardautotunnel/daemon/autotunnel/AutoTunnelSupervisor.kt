package com.zaneschepke.wireguardautotunnel.daemon.autotunnel

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.wgtunnel.backend.autotunnel.AutoTunnelHost
import com.wgtunnel.backend.autotunnel.AutoTunnelNetwork
import com.wgtunnel.backend.autotunnel.AutoTunnelNetworkType
import com.wgtunnel.backend.autotunnel.AutoTunnelPolicy
import com.wgtunnel.backend.autotunnel.AutoTunnelReconciler
import com.wgtunnel.backend.autotunnel.AutoTunnelSnapshot
import com.wgtunnel.backend.autotunnel.AutoTunnelTunnel
import com.wgtunnel.backend.autotunnel.TunnelActions
import com.wgtunnel.backend.network.NetworkInfoDto
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.NetworkStatusDto
import com.zaneschepke.wireguardautotunnel.daemon.dto.toBackendMode
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DesktopNetworkMonitor
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AutoTunnelSupervisor(
    private val backend: Backend,
    private val networkMonitor: DesktopNetworkMonitor,
    scope: CoroutineScope,
) {
    private val log = Logger.withTag("AutoTunnel")
    private val planFlow = MutableStateFlow(AutoTunnelConfigDto())
    private val runningFlow = MutableStateFlow(false)

    /**
     * Held by every start/stop, user or auto. Auto tunnel decides and acts inside it, so a user
     * action that got there first has already flagged its override.
     */
    val tunnelActionMutex = Mutex()

    private val ignoreBssid: Boolean =
        System.getProperty("os.name").orEmpty().contains("windows", ignoreCase = true)

    private val reconciler =
        AutoTunnelReconciler(scope = scope, status = backend.status, host = DaemonHost())

    val statusFlow: Flow<AutoTunnelStatusDto> =
        combine(runningFlow, planFlow, networkMonitor.info) { running, plan, network ->
                statusOf(running, plan, network)
            }
            .distinctUntilChanged()

    val status: AutoTunnelStatusDto
        get() = statusOf(runningFlow.value, planFlow.value, networkMonitor.info.value)

    suspend fun updatePlan(plan: AutoTunnelConfigDto) {
        if (planFlow.value != plan) {
            planFlow.value = plan
        }
        if (plan.enabled) start() else stop()
    }

    fun notifyUserOverride() = reconciler.notifyUserOverride()

    @OptIn(FlowPreview::class)
    fun start() {
        if (runningFlow.value) return
        runningFlow.value = true
        reconciler.start(
            combine(networkMonitor.info, planFlow) { network, plan -> snapshot(network, plan) }
                .distinctUntilChanged()
                .debounce(300.milliseconds)
        )
        log.i { "Auto-tunnel started" }
    }

    fun stop() {
        val wasRunning = runningFlow.value
        runningFlow.value = false
        reconciler.stop()
        if (wasRunning) log.i { "Auto-tunnel stopped" }
    }

    private fun snapshot(network: NetworkInfoDto, plan: AutoTunnelConfigDto): AutoTunnelSnapshot {
        val networkForEngine =
            if (ignoreBssid) network.toAutoTunnelNetwork().copy(bssid = "")
            else network.toAutoTunnelNetwork()
        return AutoTunnelSnapshot(
            network = networkForEngine,
            policy =
                AutoTunnelPolicy(
                    isTunnelOnWifiEnabled = plan.settings.isTunnelOnWifiEnabled,
                    isTunnelOnEthernetEnabled = plan.settings.isTunnelOnEthernetEnabled,
                    isWildcardsEnabled = plan.settings.isWildcardsEnabled,
                    isStopOnNoInternetEnabled = plan.settings.isStopOnNoInternetEnabled,
                    trustedNetworkSsids = plan.settings.trustedNetworkSsids,
                    trustedNetworkBssids =
                        if (ignoreBssid) emptyList() else plan.settings.trustedNetworkBssids,
                ),
            tunnels =
                plan.tunnels.map {
                    AutoTunnelTunnel(
                        id = it.id,
                        name = it.name,
                        isPrimaryTunnel = it.isPrimaryTunnel,
                        isEthernetTunnel = it.isEthernetTunnel,
                        tunnelNetworks = it.tunnelNetworks,
                        tunnelBssids = if (ignoreBssid) emptyList() else it.tunnelBssids,
                    )
                },
        )
    }

    private inner class DaemonHost : AutoTunnelHost {
        private val actions =
            object : TunnelActions {
                override suspend fun start(id: Long) {
                    val tunnelPlan = planFlow.value.tunnels.firstOrNull { it.id == id } ?: return
                    log.i { "Starting tunnel ${tunnelPlan.name}" }
                    startFromPlan(tunnelPlan)
                }

                override suspend fun stop(id: Long) {
                    log.i { "Stopping tunnel $id" }
                    backend.stop(id.toInt()).onFailure { log.e(it) { "Failed to stop tunnel $id" } }
                }
            }

        override suspend fun <T> exclusively(block: suspend (TunnelActions) -> T): T =
            tunnelActionMutex.withLock { block(actions) }
    }

    private suspend fun startFromPlan(tunnelPlan: AutoTunnelTunnelConfigDto) {
        val request = tunnelPlan.startRequest
        val config =
            runCatching { Config.parseQuickString(request.quickConfig) }
                .onFailure { log.e(it) { "Invalid auto-tunnel config for ${tunnelPlan.name}" } }
                .getOrNull() ?: return
        val tunnel = RunningTunnel.fromRequest(tunnelPlan.id.toInt(), request)
        backend
            .start(tunnel, request.toBackendMode(config), request.tunnelDns?.toCore())
            .onFailure { log.w(it) { "Failed to start auto-tunnel ${tunnelPlan.name}" } }
    }

    private fun statusOf(
        running: Boolean,
        plan: AutoTunnelConfigDto,
        network: NetworkInfoDto,
    ): AutoTunnelStatusDto {
        return AutoTunnelStatusDto(
            running = running,
            enabled = plan.enabled,
            network =
                NetworkStatusDto(
                    type = network.type,
                    ssid = network.ssid,
                    bssid = if (ignoreBssid) "" else network.bssid.uppercase(),
                ),
        )
    }
}

private fun NetworkInfoDto.toAutoTunnelNetwork(): AutoTunnelNetwork {
    val type =
        when (type.lowercase()) {
            "wifi" -> AutoTunnelNetworkType.WIFI
            "ethernet" -> AutoTunnelNetworkType.ETHERNET
            else -> AutoTunnelNetworkType.DISCONNECTED
        }
    return AutoTunnelNetwork(
        type = type,
        ssid = ssid,
        bssid = bssid.uppercase(),
        hasUsableNetwork = isUsable && type != AutoTunnelNetworkType.DISCONNECTED,
        captivePortal = false,
    )
}

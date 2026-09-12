package com.zaneschepke.wireguardautotunnel.daemon.autotunnel

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Backend
import com.wgtunnel.backend.autotunnel.AutoTunnelDecision
import com.wgtunnel.backend.autotunnel.AutoTunnelEngine
import com.wgtunnel.backend.autotunnel.AutoTunnelNetwork
import com.wgtunnel.backend.autotunnel.AutoTunnelNetworkType
import com.wgtunnel.backend.autotunnel.AutoTunnelPolicy
import com.wgtunnel.backend.autotunnel.AutoTunnelSnapshot
import com.wgtunnel.backend.autotunnel.AutoTunnelTunnel
import com.wgtunnel.backend.network.NetworkInfoDto
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelStatusDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.NetworkStatusDto
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.dto.toBackendMode
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.DesktopNetworkMonitor
import com.zaneschepke.wireguardautotunnel.daemon.tunnel.RunningTunnel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AutoTunnelSupervisor(
    private val backend: Backend,
    private val cacheRepository: DaemonCacheRepository,
    private val networkMonitor: DesktopNetworkMonitor,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("AutoTunnel")
    private val engine = AutoTunnelEngine()
    private val mutex = Mutex()
    private val planFlow = MutableStateFlow(AutoTunnelConfigDto())
    private val runningFlow = MutableStateFlow(false)

    @Volatile private var hasUserOverride = false
    private var lastNetworkKey: String? = null
    private var loopJob: Job? = null
    private var noInternetStopJob: Job? = null

    val statusFlow: Flow<AutoTunnelStatusDto> =
        combine(runningFlow, planFlow, networkMonitor.info) { running, plan, network ->
                statusOf(running, plan, network)
            }
            .distinctUntilChanged()

    val status: AutoTunnelStatusDto
        get() = statusOf(runningFlow.value, planFlow.value, networkMonitor.info.value)

    suspend fun restoreFromCache() {
        val plan = cacheRepository.getAutoTunnelPlan() ?: return
        planFlow.value = plan
        if (plan.enabled && plan.startOnBoot) {
            log.i { "Restoring auto-tunnel on boot" }
            start()
        }
    }

    suspend fun updatePlan(plan: AutoTunnelConfigDto) {
        if (planFlow.value != plan) {
            cacheRepository.updateAutoTunnelPlan(plan)
            planFlow.value = plan
        }
        if (plan.enabled) start() else stop()
    }

    fun notifyUserOverride() {
        hasUserOverride = true
        log.d { "User override on current network; pausing auto-tunnel decisions" }
    }

    fun start() {
        if (runningFlow.value) return
        runningFlow.value = true
        hasUserOverride = false
        lastNetworkKey = null
        loopJob?.cancel()
        loopJob = scope.launch { runLoop() }
        log.i { "Auto-tunnel started" }
    }

    fun stop() {
        val wasRunning = runningFlow.value
        runningFlow.value = false
        loopJob?.cancel()
        loopJob = null
        cancelNoInternetStop()
        hasUserOverride = false
        if (wasRunning) log.i { "Auto-tunnel stopped" }
    }

    @OptIn(FlowPreview::class)
    private suspend fun runLoop() {
        combine(
                networkMonitor.info,
                backend.status.map { it.activeTunnels.keys }.distinctUntilChanged(),
                planFlow,
            ) { network, activeIds, plan ->
                Triple(network, activeIds, plan)
            }
            .distinctUntilChanged()
            .debounce(300.milliseconds)
            .collect { (network, _, plan) ->
                if (!runningFlow.value || !plan.enabled) return@collect
                mutex.withLock { applyDecision(network, plan) }
            }
    }

    private fun snapshot(
        network: NetworkInfoDto,
        activeIds: Set<Int>,
        plan: AutoTunnelConfigDto,
    ): AutoTunnelSnapshot {
        return AutoTunnelSnapshot(
            network = network.toAutoTunnelNetwork(),
            policy =
                AutoTunnelPolicy(
                    isTunnelOnWifiEnabled = plan.settings.isTunnelOnWifiEnabled,
                    isTunnelOnEthernetEnabled = plan.settings.isTunnelOnEthernetEnabled,
                    isWildcardsEnabled = plan.settings.isWildcardsEnabled,
                    isStopOnNoInternetEnabled = plan.settings.isStopOnNoInternetEnabled,
                    trustedNetworkSsids = plan.settings.trustedNetworkSsids,
                    trustedNetworkBssids = plan.settings.trustedNetworkBssids,
                ),
            tunnels =
                plan.tunnels.map {
                    AutoTunnelTunnel(
                        id = it.id,
                        name = it.name,
                        isPrimaryTunnel = it.isPrimaryTunnel,
                        isEthernetTunnel = it.isEthernetTunnel,
                        tunnelNetworks = it.tunnelNetworks,
                        tunnelBssids = it.tunnelBssids,
                    )
                },
            activeTunnelIds = activeIds.map { it.toLong() }.toSet(),
        )
    }

    private fun updateFingerprint(snapshot: AutoTunnelSnapshot) {
        val bssidAware =
            snapshot.policy.trustedNetworkBssids.isNotEmpty() ||
                snapshot.tunnels.any { it.tunnelBssids.isNotEmpty() }
        val key = snapshot.network.fingerprint(bssidAware)
        if (lastNetworkKey != key) {
            if (hasUserOverride) log.d { "Network changed, clearing user override" }
            hasUserOverride = false
            lastNetworkKey = key
        }
    }

    private suspend fun applyDecision(network: NetworkInfoDto, plan: AutoTunnelConfigDto) {
        val liveActive = backend.status.first().activeTunnels.keys
        val snapshot = snapshot(network, liveActive, plan)
        updateFingerprint(snapshot)
        val event = if (hasUserOverride) AutoTunnelDecision.DoNothing else engine.evaluate(snapshot)
        log.d {
            "Decision=$event type=${snapshot.network.type} usable=${snapshot.network.hasUsableNetwork} active=${snapshot.activeTunnelIds}"
        }
        handle(event, plan)

        val followNetwork = networkMonitor.info.value
        val followActive = backend.status.first().activeTunnels.keys
        val followPlan = planFlow.value
        val followSnapshot = snapshot(followNetwork, followActive, followPlan)
        val followUp =
            if (hasUserOverride) AutoTunnelDecision.DoNothing else engine.evaluate(followSnapshot)
        if (followUp != AutoTunnelDecision.DoNothing && followUp != event) {
            log.d { "Follow-up decision=$followUp type=${followSnapshot.network.type}" }
            handle(followUp, followPlan)
        }
    }

    private suspend fun handle(event: AutoTunnelDecision, plan: AutoTunnelConfigDto) {
        when (event) {
            is AutoTunnelDecision.Sync -> {
                cancelNoInternetStop()
                event.stop.forEach { id ->
                    log.i { "Stopping tunnel $id" }
                    backend.stop(id.toInt()).onFailure { log.e(it) { "Failed to stop tunnel $id" } }
                }
                val stillActive =
                    backend.status.first().activeTunnels.keys.map { it.toLong() }.toSet()
                event.start
                    .filterNot { it in stillActive }
                    .forEach { id ->
                        val tunnelPlan = plan.tunnels.firstOrNull { it.id == id } ?: return@forEach
                        log.i { "Starting tunnel ${tunnelPlan.name}" }
                        startFromPlan(tunnelPlan)
                    }
            }
            AutoTunnelDecision.StopAllDueToNoInternet -> scheduleNoInternetStop()
            AutoTunnelDecision.DoNothing -> Unit
        }
    }

    private suspend fun startFromPlan(tunnelPlan: AutoTunnelTunnelConfigDto) {
        val request = tunnelPlan.startRequest
        val config =
            runCatching { Config.parseQuickString(request.quickConfig) }
                .onFailure { log.e(it) { "Invalid auto-tunnel config for ${tunnelPlan.name}" } }
                .getOrNull() ?: return
        val tunnel = RunningTunnel.fromRequest(tunnelPlan.id.toInt(), request)
        cacheRepository.updateLastStartRequest(tunnelPlan.id, request)
        repeat(START_ATTEMPTS) { attempt ->
            val result =
                backend.start(tunnel, request.toBackendMode(config), request.tunnelDns?.toCore())
            if (result.isSuccess) return
            log.w(result.exceptionOrNull()) {
                "Failed to start auto-tunnel ${tunnelPlan.name} (attempt ${attempt + 1}/$START_ATTEMPTS)"
            }
            if (attempt < START_ATTEMPTS - 1) delay(START_RETRY_DELAY)
        }
    }

    private fun scheduleNoInternetStop() {
        noInternetStopJob?.cancel()
        noInternetStopJob = scope.launch {
            delay(NO_INTERNET_GRACE_MS.milliseconds)
            mutex.withLock {
                val network = networkMonitor.info.value.toAutoTunnelNetwork()
                val plan = planFlow.value
                if (!network.hasUsableNetwork && plan.settings.isStopOnNoInternetEnabled) {
                    val ids = backend.status.first().activeTunnels.keys
                    if (ids.isNotEmpty()) {
                        log.w { "No internet grace expired; stopping tunnels $ids" }
                        ids.forEach { backend.stop(it) }
                    }
                } else {
                    log.d { "No internet grace expired, but internet is back or setting disabled" }
                }
            }
        }
    }

    private fun cancelNoInternetStop() {
        noInternetStopJob?.cancel()
        noInternetStopJob = null
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
                    bssid = network.bssid.uppercase(),
                ),
        )
    }

    companion object {
        private const val NO_INTERNET_GRACE_MS = 10_000L
        private const val START_ATTEMPTS = 3
        private val START_RETRY_DELAY = 400.milliseconds
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

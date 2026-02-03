package com.zaneschepke.wireguardautotunnel.tunnel

import com.zaneschepke.wireguardautotunnel.tunnel.native.AwgTunnel
import com.zaneschepke.wireguardautotunnel.tunnel.native.StatusCodeCallback
import com.zaneschepke.wireguardautotunnel.tunnel.util.BackendException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.withLock

class AmneziaBackend : Backend {
    private val tun = AwgTunnel.INSTANCE

    private var currentMode: Backend.Mode = Backend.Mode.Userspace

    private val _status = MutableStateFlow(Backend.Status(false, currentMode, emptyMap()))

    override val status: Flow<Backend.Status> = _status.asStateFlow()

    private val tunnelHandles = ConcurrentHashMap<Tunnel, Int>()
    private val tunnelJobs = ConcurrentHashMap<Tunnel, Job>()

    private val backendScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initKillSwitchStatus()
    }

    private fun initKillSwitchStatus() {
        val status = tun.getKillSwitchStatus()
        val enabled = status == 1
        _status.update { it.copy(killSwitchEnabled = enabled) }
    }

    @Synchronized
    override fun start(tunnel: Tunnel, config: String): Result<Unit> = runCatching {
            if (_status.value.activeTunnels.any { it.key.id == tunnel.id }) {
                return Result.success(Unit)
            }

            tunnel.updateState(Tunnel.State.Starting)
            _status.update { it.copy(activeTunnels = it.activeTunnels + (tunnel to Tunnel.State.Starting)) }

            val statusFlow = callbackFlow {
                val statusCallback = object : StatusCodeCallback {
                    override fun onTunnelStatusCode(handle: Int, statusCode: Int) {
                        trySend(statusCode)
                    }
                }

                val handle = when(currentMode) {
                    Backend.Mode.Proxy -> tun.awgProxyTurnOn(config, statusCallback)
                    Backend.Mode.Userspace -> tun.awgTurnOn(config, statusCallback)
                }

                if (handle < 0) {
                    close(BackendException.BackendFailure(IllegalStateException("Tunnel failed to start with handle: $handle")))
                    tunnel.updateState(Tunnel.State.Down)
                    _status.update { it.copy(activeTunnels = it.activeTunnels - tunnel) }
                } else {
                    tunnelHandles[tunnel] = handle
                    _status.update { it.copy(activeTunnels = it.activeTunnels + (tunnel to Tunnel.State.Up.Unknown)) }
                }
                awaitCancellation()
            }.buffer(Channel.BUFFERED)

            tunnelJobs[tunnel] = backendScope.launch {
                statusFlow.collect { statusCode ->
                    val tunnelState = mapStatusCodeToState(statusCode)
                    _status.update { it.copy(activeTunnels = it.activeTunnels + (tunnel to tunnelState)) }
                    tunnel.updateState(tunnelState)
                }
            }.apply { invokeOnCompletion {
                tunnelJobs.remove(tunnel)
            } }
        }.onFailure { throwable ->
            tunnel.updateState(Tunnel.State.Down)
            tunnelJobs.remove(tunnel)?.cancel()
            _status.update { it.copy(activeTunnels = it.activeTunnels - tunnel) }
            return Result.failure(BackendException.BackendFailure(throwable))
        }

    @Synchronized
    override fun stop(id: Int) {
        val tunnel = tunnelHandles.keys.firstOrNull { t -> t.id == id } ?: return
        val handle = tunnelHandles.remove(tunnel) ?: return

        when(currentMode) {
            Backend.Mode.Proxy -> tun.awgProxyTurnOff(handle)
            Backend.Mode.Userspace -> tun.awgTurnOff(handle)
        }

        tunnelJobs.remove(tunnel)?.cancel()

        tunnel.updateState(Tunnel.State.Down)
        _status.update { it.copy(activeTunnels = it.activeTunnels - tunnel) }
    }

    override fun setMode(mode: Backend.Mode) {
        if (mode == currentMode) return
        shutdown()
        currentMode = mode
    }

    override fun shutdown() {

        when(currentMode) {
            Backend.Mode.Proxy -> tun.awgProxyTurnOffAll()
            Backend.Mode.Userspace -> tun.awgTurnOffAll()
        }

        tunnelJobs.values.forEach { it.cancel() }
        tunnelJobs.clear()
        tunnelHandles.clear()
        _status.update { it.copy(activeTunnels = emptyMap()) }
    }

    override fun setKillSwitch(enabled: Boolean): Result<Unit> {
        if (_status.value.killSwitchEnabled == enabled) return Result.success(Unit)
        val setValue = if (enabled) 1 else 0
        val status = tun.setKillSwitch(setValue)
        if (status == -1) return Result.failure(BackendException.KillSwitchSetFailed(""))
        val killSwitchEnabled = status == 1
        _status.update { it.copy(killSwitchEnabled = killSwitchEnabled) }
        return Result.success(Unit)
    }

    private fun mapStatusCodeToState(statusCode: Int): Tunnel.State {
        // Matching native status codes
        return when (statusCode) {
            0 -> Tunnel.State.Up.Healthy
            1 -> Tunnel.State.Up.HandshakeFailure
            2 -> Tunnel.State.Up.ResolvingDns
            3 -> Tunnel.State.Up.Unknown
            else -> Tunnel.State.Down  // unknow or negative error code consider down
        }
    }
}
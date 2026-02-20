package com.zaneschepke.wireguardautotunnel.tunnel

import co.touchlab.kermit.Logger
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.zaneschepke.wireguardautotunnel.tunnel.model.TunnelKey
import com.zaneschepke.wireguardautotunnel.tunnel.native.AwgTunnel
import com.zaneschepke.wireguardautotunnel.tunnel.native.StatusCodeCallback
import com.zaneschepke.wireguardautotunnel.tunnel.util.BackendException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.firstOrNull

class AmneziaBackend : Backend {
    private val tun = AwgTunnel.INSTANCE
    private val log = Logger.withTag("AmneziaBackend")

    private val tunnelMutex = Mutex()
    private val killSwitchMutex = Mutex()

    private var currentMode: Backend.Mode = Backend.Mode.Userspace

    private val _status = MutableStateFlow(
        Backend.Status(
            killSwitchEnabled = false,
            killSwitchLanBypassEnabled = false,
            mode = currentMode,
            activeTunnels = emptyMap(),
        )
    )

    override val status: Flow<Backend.Status> = _status.asStateFlow()

    private val tunnelHandles = ConcurrentHashMap<Long, Int>()
    private val tunnelJobs = ConcurrentHashMap<Long, Job>()
    private val statusCallbacks = ConcurrentHashMap<Long, StatusCodeCallback>()

    private val backendScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        backendScope.launch { initKillSwitchStatus() }
    }

    private suspend fun initKillSwitchStatus() = killSwitchMutex.withLock {
        log.d { "Initializing kill switch status..." }
        val killSwitchStatus = tun.getKillSwitchStatus()
        val killSwitchEnabled = killSwitchStatus == 1
        val bypassEnabled = if (killSwitchEnabled) {
            tun.getKillSwitchLanBypassStatus() == 1
        } else false

        _status.update {
            it.copy(killSwitchEnabled = killSwitchEnabled, killSwitchLanBypassEnabled = bypassEnabled)
        }
        log.d { "Kill switch initialized: enabled=$killSwitchEnabled, bypass=$bypassEnabled" }
    }

    override suspend fun start(tunnel: Tunnel, config: String): Result<Unit> = runCatching {
        log.i { "Start request for tunnel: ${tunnel.id}" }


        val statusChannel = Channel<Int>(Channel.BUFFERED)
        val statusCallback = object : StatusCodeCallback {
            override fun onTunnelStatusCode(handle: Int, statusCode: Int) {
                log.v { "Native Callback - Handle: $handle, Code: $statusCode" }
                statusChannel.trySend(statusCode)
            }
        }

        statusCallbacks[tunnel.id] = statusCallback

        val handle = tunnelMutex.withLock {
            if (tunnelHandles.containsKey(tunnel.id)) {
                log.w { "Tunnel ${tunnel.id} already exists in handles map." }
                throw BackendException.StateConflict("Tunnel ${tunnel.id} is already in use")
            }

            log.d { "Lock acquired. Invoking native turnOn..." }
            val nativeHandle = when (currentMode) {
                Backend.Mode.Proxy -> tun.awgProxyTurnOn(config, statusCallback)
                Backend.Mode.Userspace -> tun.awgTurnOn(config, statusCallback)
            }

            if (nativeHandle < 0) {
                log.e { "Native turnOn failed: $nativeHandle" }
                throw BackendException.InternalError("Tunnel failed with internal error code $nativeHandle")
            }

            tunnelHandles[tunnel.id] = nativeHandle
            nativeHandle
        }

        log.i { "Tunnel ${tunnel.id} native initialization successful. Handle: $handle" }

        tunnel.updateState(Tunnel.State.Starting)
        _status.update {
            it.copy(
                activeTunnels = it.activeTunnels +
                        (TunnelKey(tunnel.id, tunnel.name) to Tunnel.State.Starting)
            )
        }

        tunnelJobs[tunnel.id] = backendScope.launch {
            try {
                statusChannel.consumeAsFlow().collect { statusCode ->
                    val tunnelState = mapStatusCodeToState(statusCode)
                    log.d { "Tunnel ${tunnel.id} status update: $statusCode -> $tunnelState" }

                    _status.update {
                        it.copy(activeTunnels = it.activeTunnels +
                                (TunnelKey(tunnel.id, tunnel.name) to tunnelState))
                    }
                    tunnel.updateState(tunnelState)
                }
            } catch (e: Exception) {
                log.e(e) { "Error in status flow for tunnel ${tunnel.id}" }
            } finally {
                log.i { "Status collector for tunnel ${tunnel.id} terminating." }
                statusChannel.close()
                cleanupTunnelState(tunnel.id)
                tunnel.updateState(Tunnel.State.Down)
            }
        }
    }

    override suspend fun stop(id: Long): Result<Unit> = runCatching {
        log.i { "Stop request for tunnel ID: $id" }

        val handle = tunnelHandles[id] ?: run {
            log.w { "Stop requested for $id but no handle found." }
            return Result.failure(BackendException.StateConflict("Tunnel $id is not active."))
        }
        _status.update { current ->
            val key = current.activeTunnels.keys.firstOrNull { it.id == id }
            if (key != null) current.copy(
                activeTunnels = current.activeTunnels + (key to Tunnel.State.Stopping)
            )
            else current
        }


        tunnelMutex.withLock {
            log.d { "Lock acquired for Stop. Calling native turnOff for handle: $handle" }
            when (currentMode) {
                Backend.Mode.Proxy -> tun.awgProxyTurnOff(handle)
                Backend.Mode.Userspace -> tun.awgTurnOff(handle)
            }
        }

        tunnelJobs[id]?.cancel()

        log.i { "Stop command sent and job cancelled for tunnel $id" }
    }

    private fun cleanupTunnelState(id: Long) {
        statusCallbacks.remove(id)
        tunnelJobs.remove(id)
        tunnelHandles.remove(id)
        _status.update { current ->
            val key = current.activeTunnels.keys.firstOrNull { it.id == id }
            if (key != null) current.copy(activeTunnels = current.activeTunnels - key)
            else current
        }
    }

    override fun shutdown() {
        log.i { "Backend shutdown initiated" }
        when (currentMode) {
            Backend.Mode.Proxy -> tun.awgProxyTurnOffAll()
            Backend.Mode.Userspace -> tun.awgTurnOffAll()
        }
        tunnelJobs.values.forEach { it.cancel() }
        tunnelJobs.clear()
        tunnelHandles.clear()
        statusCallbacks.clear()
        _status.update { it.copy(activeTunnels = emptyMap()) }
    }

    override suspend fun getActiveConfig(id: Long): Result<String?> {
        val handle =
            tunnelHandles.keys.find { it == id }?.let { tunnelHandles[it] }
                ?: return Result.failure(
                    BackendException.StateConflict("Tunnel with $id is not active.")
                )
        val pointer: Pointer =
            AwgTunnel.INSTANCE.awgGetConfig(handle) ?: return Result.success(null)
        return try {
            val configStr = pointer.getString(0L)
            Result.success(configStr)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to get config for id: $id" }
            Result.success(null)
        } finally {
            Native.free(Pointer.nativeValue(pointer))
        }
    }

    override suspend fun setMode(mode: Backend.Mode) {
        if (mode == currentMode) return
        shutdown()
        currentMode = mode
    }


    override suspend fun setKillSwitch(enabled: Boolean): Result<Unit> {
        if (_status.value.killSwitchEnabled == enabled)
            return Result.failure(
                BackendException.StateConflict("Kill switch enable: $enabled is already set.")
            )
        val killSwitchEnabled =
            killSwitchMutex.withLock {
                val setValue = if (enabled) 1 else 0
                val status = tun.setKillSwitch(setValue)
                if (status == -1)
                    return Result.failure(
                        BackendException.InternalError(
                            "Kill switch failed to start with error code: $status"
                        )
                    )
                status == 1
            }
        _status.update { it.copy(killSwitchEnabled = killSwitchEnabled) }
        return Result.success(Unit)
    }

    override suspend fun setKillSwitchLanBypass(enabled: Boolean): Result<Unit> {
        if (!_status.value.killSwitchEnabled)
            return Result.failure(BackendException.StateConflict("Kill switch is not active."))
        killSwitchMutex.withLock {
            val setValue = if (enabled) 1 else 0
            tun.setKillSwitchLanBypass(setValue)
        }
        return Result.success(Unit)
    }

    private fun mapStatusCodeToState(statusCode: Int): Tunnel.State {
        return when (statusCode) {
            0 -> Tunnel.State.Up.Healthy
            1 -> Tunnel.State.Up.HandshakeFailure
            2 -> Tunnel.State.Up.ResolvingDns
            3 -> Tunnel.State.Up.Unknown
            else -> Tunnel.State.Down
        }
    }
}

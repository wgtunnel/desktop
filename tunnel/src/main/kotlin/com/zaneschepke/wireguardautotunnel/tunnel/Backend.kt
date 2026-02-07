package com.zaneschepke.wireguardautotunnel.tunnel

import com.zaneschepke.wireguardautotunnel.tunnel.model.TunnelKey
import kotlinx.coroutines.flow.Flow

interface Backend {
    fun start(tunnel: Tunnel, config : String) : Result<Unit>
    fun stop(id : Long) : Result<Unit>
    fun setMode(mode: Mode)

    fun setKillSwitch(enabled: Boolean) : Result<Unit>

    fun shutdown()

    val status : Flow<Status>

    sealed interface Mode {
        data object Userspace: Mode
        data object Proxy : Mode
    }

    data class Status(
        val killSwitchEnabled: Boolean,
        val mode: Mode,
        val activeTunnels: Map<TunnelKey, Tunnel.State>
    )
}
package com.zaneschepke.wireguardautotunnel.tunnel

import kotlinx.coroutines.flow.Flow

interface Backend {
    fun start(tunnel: Tunnel, config : String) : Result<Unit>
    fun stop(id : Int)
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
        val activeTunnels: Map<Tunnel, Tunnel.State>
    )
}
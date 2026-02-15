package com.zaneschepke.wireguardautotunnel.tunnel

import com.zaneschepke.wireguardautotunnel.tunnel.model.TunnelKey
import kotlinx.coroutines.flow.Flow

interface Backend {
    suspend fun start(tunnel: Tunnel, config: String): Result<Unit>

    suspend fun stop(id: Long): Result<Unit>

    suspend fun setMode(mode: Mode)

    suspend fun setKillSwitch(enabled: Boolean): Result<Unit>

    suspend fun setKillSwitchLanBypass(enabled: Boolean): Result<Unit>

    fun shutdown()

    suspend fun getActiveConfig(id: Long): Result<String?>

    val status: Flow<Status>

    sealed interface Mode {
        data object Userspace : Mode

        data object Proxy : Mode
    }

    data class Status(
        val killSwitchEnabled: Boolean,
        val killSwitchLanBypassEnabled: Boolean,
        val mode: Mode,
        val activeTunnels: Map<TunnelKey, Tunnel.State>,
    )
}

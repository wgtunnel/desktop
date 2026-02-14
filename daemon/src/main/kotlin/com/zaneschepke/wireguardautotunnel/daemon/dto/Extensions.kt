package com.zaneschepke.wireguardautotunnel.daemon.dto

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import com.zaneschepke.wireguardautotunnel.tunnel.Backend
import com.zaneschepke.wireguardautotunnel.tunnel.Tunnel

fun Tunnel.State.toDto(): TunnelState =
    when (this) {
        Tunnel.State.Down -> TunnelState.DOWN
        Tunnel.State.Starting -> TunnelState.STARTING
        is Tunnel.State.Up.Healthy -> TunnelState.HEALTHY
        is Tunnel.State.Up.HandshakeFailure -> TunnelState.HANDSHAKE_FAILURE
        is Tunnel.State.Up.ResolvingDns -> TunnelState.RESOLVING_DNS
        is Tunnel.State.Up.Unknown -> TunnelState.UNKNOWN
    }

fun Backend.Mode.toDto(): BackendMode =
    when (this) {
        Backend.Mode.Userspace -> BackendMode.USERSPACE
        Backend.Mode.Proxy -> BackendMode.PROXY
    }

fun BackendMode.toInternal(): Backend.Mode =
    when (this) {
        BackendMode.USERSPACE -> Backend.Mode.Userspace
        BackendMode.PROXY -> Backend.Mode.Proxy
    }

fun Backend.Status.toDto(): BackendStatus {
    val activeList =
        activeTunnels.map { (key, state) ->
            TunnelStatus(id = key.id, name = key.name, state = state.toDto())
        }
    return BackendStatus(
        killSwitchEnabled = killSwitchEnabled,
        mode = mode.toDto(),
        activeTunnels = activeList,
    )
}

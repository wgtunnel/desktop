package com.zaneschepke.wireguardautotunnel.daemon.dto

import com.wgtunnel.backend.Tunnel
import com.wgtunnel.backend.model.BackendMode as CoreBackendMode
import com.wgtunnel.backend.model.KillSwitchConfig
import com.wgtunnel.backend.model.ProxyConfig
import com.wgtunnel.backend.model.dns.ForeignDnsPolicy
import com.wgtunnel.backend.model.dns.TunnelDnsConfig
import com.wgtunnel.backend.state.ActiveTunnel
import com.wgtunnel.backend.state.BackendStatus as CoreBackendStatus
import com.wgtunnel.backend.state.BootstrapState
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.ProxyConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelDnsConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelModeDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelState
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest

fun Tunnel.State.toDto(bootstrap: BootstrapState = BootstrapState.None): TunnelState {
    if (bootstrap is BootstrapState.ResolvingDns) return TunnelState.RESOLVING_DNS
    return when (this) {
        Tunnel.State.Down -> TunnelState.DOWN
        Tunnel.State.Starting -> TunnelState.STARTING
        Tunnel.State.Stopping -> TunnelState.STOPPING
        is Tunnel.State.Up.Healthy -> TunnelState.HEALTHY
        is Tunnel.State.Up.HandshakeFailure -> TunnelState.HANDSHAKE_FAILURE
    }
}

fun CoreBackendMode?.toDto(): BackendMode =
    when (this) {
        is CoreBackendMode.Vpn -> BackendMode.VPN
        is CoreBackendMode.Proxy.Standard -> BackendMode.PROXY
        is CoreBackendMode.Proxy.KillSwitchPrimary -> BackendMode.LOCK_DOWN
        null -> BackendMode.UNKNOWN
    }

fun KillSwitchConfigDto.toCore(): KillSwitchConfig =
    KillSwitchConfig(allowedIps = allowedIps, metered = false, dualStack = dualStack)

fun ProxyConfigDto.toCore(): ProxyConfig =
    ProxyConfig(
        socks5 =
            socks5?.let {
                ProxyConfig.Socks5(
                    host = it.host,
                    port = it.port,
                    username = it.username,
                    password = it.password,
                )
            },
        http =
            http?.let {
                ProxyConfig.Http(
                    host = it.host,
                    port = it.port,
                    username = it.username,
                    password = it.password,
                )
            },
    )

fun TunnelDnsConfigDto.toCore(): TunnelDnsConfig =
    TunnelDnsConfig(
        defaultTransport = defaultTransport,
        localSuffixes = localSuffixes,
        upstream = upstream,
        serverName = serverName,
        foreignDnsPolicy =
            when (foreignDnsPolicy.trim().uppercase()) {
                "BLOCK",
                "DROP" -> ForeignDnsPolicy.BLOCK
                "ALLOW" -> ForeignDnsPolicy.ALLOW
                else -> ForeignDnsPolicy.REDIRECT
            },
        splitMode =
            when (splitMode.trim().uppercase()) {
                "TUNNEL" -> com.wgtunnel.backend.model.dns.DnsSplitMode.TUNNEL
                else -> com.wgtunnel.backend.model.dns.DnsSplitMode.SYSTEM
            },
    )

fun StartTunnelRequest.toBackendMode(config: Config): CoreBackendMode {
    return when (mode) {
        TunnelModeDto.VPN -> CoreBackendMode.Vpn(config)
        TunnelModeDto.PROXY ->
            CoreBackendMode.Proxy.Standard(
                config = config,
                proxyConfig = proxy?.toCore() ?: ProxyConfig(),
            )
        TunnelModeDto.LOCK_DOWN ->
            CoreBackendMode.Proxy.KillSwitchPrimary(
                config = config,
                killSwitchConfig =
                    killSwitch?.toCore() ?: KillSwitchConfig(emptySet(), false, false),
            )
    }
}

fun ActiveTunnel.toDto(id: Int): TunnelStatus {
    val name = tunnel?.name ?: "tunnel-$id"
    return TunnelStatus(
        id = id.toLong(),
        name = name,
        state = transportState.toDto(bootstrapState),
        mode = mode.toDto(),
        activeConfig = activeConfig,
        recoveryAttempts = recoveryAttempts,
    )
}

fun CoreBackendStatus.toDto(): BackendStatus {
    val activeList = activeTunnels.map { (id, tunnel) -> tunnel.toDto(id) }
    return BackendStatus(killSwitchEnabled = killSwitch.enabled, activeTunnels = activeList)
}

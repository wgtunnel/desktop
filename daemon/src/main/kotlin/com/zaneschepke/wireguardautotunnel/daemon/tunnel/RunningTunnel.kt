package com.zaneschepke.wireguardautotunnel.daemon.tunnel

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.TunnelFeaturesDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest

private val log = Logger.withTag("RunningTunnel")

class RunningTunnel(
    private val tunnelId: Int,
    override val name: String,
    override val isMetered: Boolean,
    override val scriptsEnabled: Boolean,
    override val ipStrategy: Tunnel.IpStrategy,
    override val features: Set<Tunnel.Feature>,
) : Tunnel {

    override val id: Int
        get() = tunnelId

    override fun updateState(state: Tunnel.State) {
        log.i { "Tunnel $id ($name) state changed → $state" }
    }

    companion object {
        fun fromRequest(id: Int, request: StartTunnelRequest): RunningTunnel {
            val features = buildSet {
                if (request.features.statisticsEnabled) {
                    add(
                        Tunnel.Feature.ActiveConfigMonitor(
                            request.features.statisticsPollIntervalSeconds
                        )
                    )
                }
                add(
                    Tunnel.Feature.Recovery(
                        seamlessRecovery = request.features.seamlessRecoveryEnabled,
                        dynamicDnsRecovery = request.features.dynamicDnsRecovery,
                        ipv4Fallback = request.preferIpv6,
                        ipv6Recovery = request.ipv6RestoreEnabled,
                    )
                )
            }
            val strategy =
                if (request.preferIpv6) {
                    Tunnel.IpStrategy.PreferIpv6(recoveryEnabled = request.ipv6RestoreEnabled)
                } else {
                    Tunnel.IpStrategy.Ipv4Only
                }
            return RunningTunnel(
                tunnelId = id,
                name = request.name,
                // Android only
                isMetered = false,
                // Always allow scripts if they exist for desktop
                scriptsEnabled = true,
                ipStrategy = strategy,
                features = features,
            )
        }

        fun fromFeatures(
            id: Int,
            name: String,
            features: TunnelFeaturesDto = TunnelFeaturesDto(),
            preferIpv6: Boolean = false,
            ipv6RestoreEnabled: Boolean = false,
        ): RunningTunnel {
            return fromRequest(
                id,
                StartTunnelRequest(
                    name = name,
                    quickConfig = "",
                    features = features,
                    preferIpv6 = preferIpv6,
                    ipv6RestoreEnabled = ipv6RestoreEnabled,
                ),
            )
        }
    }
}

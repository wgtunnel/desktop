package com.zaneschepke.wireguardautotunnel.daemon.tunnel

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.tunnel.Tunnel

class RunningTunnel(
    override val id: Int,
    override val name: String,
    override val features: Set<Tunnel.Feature> = emptySet()
) : Tunnel {

    override fun updateState(state: Tunnel.State) {
        Logger.i { "Tunnel $id ($name) state changed → $state" }
    }
}
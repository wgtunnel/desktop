package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup

sealed class DeleteIntent {
    data class Tunnel(val tunnel: TunnelConfig) : DeleteIntent()

    data class Group(val group: TunnelGroup) : DeleteIntent()

    data class GroupAndTunnels(val group: TunnelGroup) : DeleteIntent()

    object Selected : DeleteIntent()
}

sealed class ExportIntent {
    data class Tunnel(val tunnel: TunnelConfig) : ExportIntent()

    data class Group(val group: TunnelGroup) : ExportIntent()

    object Selected : ExportIntent()
}

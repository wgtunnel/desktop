package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig

sealed class DeleteIntent {
    data class Tunnel(val tunnel: TunnelConfig) : DeleteIntent()

    object Selected : DeleteIntent()
}

sealed class ExportIntent {
    data class Tunnel(val tunnel: TunnelConfig) : ExportIntent()

    object Selected : ExportIntent()
}

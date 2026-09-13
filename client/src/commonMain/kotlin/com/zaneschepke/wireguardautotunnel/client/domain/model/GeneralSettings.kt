package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    val id: Long = 1L,
    val theme: Theme = Theme.DARK,
    val locale: String? = null,
    val alreadyDonated: Boolean = false,
    val restoreTunnelOnBoot: Boolean = false,
    val useSystemColors: Boolean = false,
    val tunnelMode: TunnelMode = TunnelMode.VPN,
    val seamlessRecoveryEnabled: Boolean = true,
    val seamlessRecoveryBounceDelaySec: Int = 30,
    val isGlobalAmneziaEnabled: Boolean = false,
    val lastNotifiedUpdateVersion: String? = null,
) {
    val selectableTunnelMode: TunnelMode
        get() =
            when (tunnelMode) {
                TunnelMode.PROXY -> TunnelMode.PROXY
                TunnelMode.VPN -> TunnelMode.VPN
            }
}

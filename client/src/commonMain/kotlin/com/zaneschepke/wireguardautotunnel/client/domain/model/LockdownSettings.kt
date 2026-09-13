package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.wgtunnel.backend.model.KillSwitchConfig
import com.wgtunnel.parser.util.AllowedIpsCalculator
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import kotlinx.serialization.Serializable

@Serializable
data class LockdownSettings(
    val id: Long = 1L,
    val enabled: Boolean = false,
    val restoreOnBoot: Boolean = false,
    val bypassLan: Boolean = false,
) {
    fun toKillSwitchConfig(dnsServers: Collection<String> = emptyList()): KillSwitchConfig {
        return KillSwitchConfig(
            allowedIps =
                if (bypassLan) AllowedIpsCalculator.calculateLanBypass(dnsServers).toSet()
                else emptySet(),
            metered = false,
            dualStack = true,
        )
    }

    fun toDto(dnsServers: Collection<String> = emptyList()): KillSwitchConfigDto {
        val core = toKillSwitchConfig(dnsServers)
        return KillSwitchConfigDto(allowedIps = core.allowedIps, dualStack = true)
    }
}

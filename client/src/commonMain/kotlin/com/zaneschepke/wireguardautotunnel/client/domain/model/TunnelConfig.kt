package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.zaneschepke.wireguardautotunnel.parser.Config
import kotlinx.serialization.Serializable
import kotlin.collections.get

@Serializable
data class TunnelConfig(
    val id: Int = 0,
    val name: String,
    val quickConfig: String,
    val tunnelNetworks: Set<String> = setOf(),
    val isPrimaryTunnel: Boolean = false,
    val active: Boolean = false,
    val pingTarget: String? = null,
    val isEthernetTunnel: Boolean = false,
    val isIpv4Preferred: Boolean = true,
    val position: Int = 0,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TunnelConfig) return false
        return id == other.id &&
            name == other.name &&
            quickConfig == other.quickConfig &&
            isPrimaryTunnel == other.isPrimaryTunnel &&
            isEthernetTunnel == other.isEthernetTunnel &&
            pingTarget == other.pingTarget &&
            tunnelNetworks == other.tunnelNetworks &&
            isIpv4Preferred == other.isIpv4Preferred
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + quickConfig.hashCode()
        return result
    }

    fun asConfig(): Config {
        return Config.parseQuickString(quickConfig)
    }

    companion object {

        fun generateRandom8Digits(): String {
            val digits = ('0'..'9').toList()
            return (1..8).map { digits.random() }.joinToString("")
        }

        private fun generateDefaultTunnelName(config: Config? = null): String {
            return config?.peers[0]?.host ?: generateRandom8Digits()
        }

        fun configFromQuick(quick: String): Config {
            return Config.parseQuickString(quick)
        }

        fun fromQuickString(quick: String, name: String? = null): TunnelConfig {
            val config = configFromQuick(quick)
            return tunnelConfFromConfig(config, name)
        }

        private fun tunnelConfFromConfig(config: Config, name: String? = null): TunnelConfig {
            return TunnelConfig(
                name = name ?: generateDefaultTunnelName(config),
                quickConfig = config.asQuickString(),
            )
        }
        private const val IPV6_ALL_NETWORKS = "::/0"
        private const val IPV4_ALL_NETWORKS = "0.0.0.0/0"
        val ALL_IPS = listOf(IPV4_ALL_NETWORKS, IPV6_ALL_NETWORKS)
        val IPV4_PUBLIC_NETWORKS =
            setOf(
                "0.0.0.0/5",
                "8.0.0.0/7",
                "11.0.0.0/8",
                "12.0.0.0/6",
                "16.0.0.0/4",
                "32.0.0.0/3",
                "64.0.0.0/2",
                "128.0.0.0/3",
                "160.0.0.0/5",
                "168.0.0.0/6",
                "172.0.0.0/12",
                "172.32.0.0/11",
                "172.64.0.0/10",
                "172.128.0.0/9",
                "173.0.0.0/8",
                "174.0.0.0/7",
                "176.0.0.0/4",
                "192.0.0.0/9",
                "192.128.0.0/11",
                "192.160.0.0/13",
                "192.169.0.0/16",
                "192.170.0.0/15",
                "192.172.0.0/14",
                "192.176.0.0/12",
                "192.192.0.0/10",
                "193.0.0.0/8",
                "194.0.0.0/7",
                "196.0.0.0/6",
                "200.0.0.0/5",
                "208.0.0.0/4",
            )
        val LAN_BYPASS_ALLOWED_IPS = setOf(IPV6_ALL_NETWORKS) + IPV4_PUBLIC_NETWORKS
    }
}

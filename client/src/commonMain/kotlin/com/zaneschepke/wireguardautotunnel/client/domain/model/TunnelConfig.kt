package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.wgtunnel.parser.Config
import com.wgtunnel.parser.InterfaceSection
import com.wgtunnel.parser.PeerSection
import com.wgtunnel.parser.crypto.Key
import kotlinx.serialization.Serializable

@Serializable
data class TunnelConfig(
    val id: Long = 0,
    val name: String,
    val quickConfig: String,
    val isPrimaryTunnel: Boolean = false,
    val position: Int = 0,
    val preferIpv6: Boolean = false,
    val ipv6RestoreEnabled: Boolean = false,
    val isDdnsTunnel: Boolean = false,
    val tunnelNetworks: List<String> = emptyList(),
    val isEthernetTunnel: Boolean = false,
    val tunnelBssids: List<String> = emptyList(),
) {

    fun asConfig(): Config {
        return Config.parseQuickString(quickConfig)
    }

    val isGlobalConfig: Boolean
        get() = name == GLOBAL_CONFIG_NAME

    companion object {

        const val DEFAULT_TUNNEL_NAME = "tunnel"
        const val GLOBAL_CONFIG_NAME = "4675ab06-903a-438b-8485-6ea4187a9512"

        val Empty = TunnelConfig(name = DEFAULT_TUNNEL_NAME, quickConfig = "")

        fun configFromQuick(quick: String): Config {
            return Config.parseQuickString(quick)
        }

        fun fromQuickString(quick: String, name: String? = null): TunnelConfig {
            val config = configFromQuick(quick)
            config.validate()
            return tunnelConfFromConfig(config, name)
        }

        private fun tunnelConfFromConfig(config: Config, name: String? = null): TunnelConfig {
            return TunnelConfig(
                name = name ?: DEFAULT_TUNNEL_NAME,
                quickConfig = config.asQuickString(),
            )
        }

        fun generateDefaultGlobalConfig(): TunnelConfig {
            val privateKey = Key.generatePrivateKey().toBase64()
            val publicKey = Config.generatePublicKeyFromPrivateKey(privateKey)
            val config =
                Config(
                    `interface` =
                        InterfaceSection(address = "10.0.0.2/32", privateKey = privateKey),
                    peers =
                        listOf(
                            PeerSection(
                                publicKey = publicKey,
                                endpoint = "server.example.com:51820",
                                allowedIPs = "0.0.0.0/0",
                            )
                        ),
                )
            return TunnelConfig(name = GLOBAL_CONFIG_NAME, quickConfig = config.asQuickString())
        }
    }
}

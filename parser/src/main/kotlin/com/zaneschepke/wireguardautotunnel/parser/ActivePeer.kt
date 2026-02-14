package com.zaneschepke.wireguardautotunnel.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivePeer(
    @SerialName("PublicKey") val publicKey: String,
    @SerialName("AllowedIPs") val allowedIPs: String? = null,
    @SerialName("Endpoint") val endpoint: String? = null,
    @SerialName("PresharedKey") val presharedKey: String? = null,
    @SerialName("PersistentKeepalive") val persistentKeepalive: Int? = null,
    @SerialName("LastHandshakeSeconds") val lastHandshakeSeconds: Long? = null,
    @SerialName("LastHandshakeNanos") val lastHandshakeNanos: Long? = null,
    @SerialName("TxBytes") val txBytes: Long? = null,
    @SerialName("RxBytes") val rxBytes: Long? = null,
) {

    val host: String?
        get() {
            val (h, _) = endpoint?.let { Config.parseEndpoint(it) } ?: return null
            return h
        }

    val port: Int?
        get() {
            val (_, p) = endpoint?.let { Config.parseEndpoint(it) } ?: return null
            return p?.toIntOrNull()
        }
}

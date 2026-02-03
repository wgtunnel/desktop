package com.zaneschepke.wireguardautotunnel.parser

import com.zaneschepke.wireguardautotunnel.parser.util.NetworkUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PeerSection(
    @SerialName("PublicKey") val publicKey: String,
    @SerialName("AllowedIPs") val allowedIPs: String? = null,
    @SerialName("Endpoint") val endpoint: String? = null,
    @SerialName("PresharedKey") val presharedKey: String? = null,
    @SerialName("PersistentKeepalive") val persistentKeepalive: Int? = null
) {

    @Throws(ConfigParseException::class)
    fun validate(index: Int) {
        val prefix = "Peer[$index]"
        if (publicKey.isBlank()) throw ConfigParseException(ErrorType.MISSING_REQUIRED_FIELD, "$prefix.PublicKey")
        if (!NetworkUtils.isValidBase64(publicKey)) throw ConfigParseException(ErrorType.INVALID_BASE64_KEY, "$prefix.PublicKey", publicKey)

        persistentKeepalive?.let { if (it !in 0..65535) throw ConfigParseException(ErrorType.INVALID_KEEPALIVE_NEGATIVE, "$prefix.PersistentKeepalive", it) }

        endpoint?.let {
            val (host, portStr) = Config.parseEndpoint(it)
            val port = portStr?.toIntOrNull()
            if (host == null || port == null || port !in 0..65535) {
                throw ConfigParseException(ErrorType.INVALID_ENDPOINT_FORMAT, "$prefix.Endpoint", it)
            }
            if (!NetworkUtils.isValidDnsEntry(host)) {
                throw ConfigParseException(ErrorType.INVALID_HOSTNAME, "$prefix.Endpoint host", host)
            }
        }

        allowedIPs?.split(",")?.map { it.trim() }?.forEach {
            if (it.isNotBlank() && !NetworkUtils.isValidCidr(it)) {
                throw ConfigParseException(ErrorType.INVALID_CIDR, "$prefix.AllowedIPs", it)
            }
        }
    }

    val host: String? get() {
        val (h, _) = endpoint?.let { Config.parseEndpoint(it) } ?: return null
        return h
    }

    val port: Int? get() {
        val (_, p) = endpoint?.let { Config.parseEndpoint(it) } ?: return null
        return p?.toIntOrNull()
    }
    val isStaticallyConfigured: Boolean
        get() = host?.let { NetworkUtils.isValidIp(it) } ?: false
}
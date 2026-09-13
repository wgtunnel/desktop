package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.wgtunnel.backend.model.ProxyConfig
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.ProxyConfigDto
import kotlinx.serialization.Serializable

@Serializable
data class ProxySettings(
    val id: Long = 1L,
    val socks5ProxyEnabled: Boolean = true,
    val socks5ProxyBindAddress: String? = null,
    val httpProxyEnabled: Boolean = true,
    val httpProxyBindAddress: String? = null,
    val proxyUsername: String? = null,
    val proxyPassword: String? = null,
) {
    fun toProxyConfig(): ProxyConfig {
        val socks5 =
            if (socks5ProxyEnabled) {
                parseAddress(socks5ProxyBindAddress ?: DEFAULT_SOCKS_BIND_ADDRESS)?.let {
                    (host, port) ->
                    ProxyConfig.Socks5(
                        host = host,
                        port = port,
                        username = proxyUsername,
                        password = proxyPassword,
                    )
                }
            } else null

        val http =
            if (httpProxyEnabled) {
                parseAddress(httpProxyBindAddress ?: DEFAULT_HTTP_BIND_ADDRESS)?.let { (host, port)
                    ->
                    ProxyConfig.Http(
                        host = host,
                        port = port,
                        username = proxyUsername,
                        password = proxyPassword,
                    )
                }
            } else null

        return ProxyConfig(socks5 = socks5, http = http)
    }

    fun toDto(): ProxyConfigDto {
        val core = toProxyConfig()
        return ProxyConfigDto(
            socks5 =
                core.socks5?.let {
                    ProxyConfigDto.Socks5(it.host, it.port, it.username, it.password)
                },
            http =
                core.http?.let { ProxyConfigDto.Http(it.host, it.port, it.username, it.password) },
        )
    }

    private fun parseAddress(address: String): Pair<String, Int>? {
        val parts = address.split(":")
        if (parts.size != 2) return null
        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: return null
        return host to port
    }

    companion object {
        const val DEFAULT_SOCKS_BIND_ADDRESS = "127.0.0.1:25344"
        const val DEFAULT_HTTP_BIND_ADDRESS = "127.0.0.1:25345"
    }
}

package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProxyConfigDto(val socks5: Socks5? = null, val http: Http? = null) {
    @Serializable
    data class Socks5(
        val host: String = "127.0.0.1",
        val port: Int = 25344,
        val username: String? = null,
        val password: String? = null,
    )

    @Serializable
    data class Http(
        val host: String = "127.0.0.1",
        val port: Int = 25345,
        val username: String? = null,
        val password: String? = null,
    )
}

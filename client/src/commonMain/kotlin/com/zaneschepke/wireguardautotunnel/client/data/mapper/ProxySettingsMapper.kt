package com.zaneschepke.wireguardautotunnel.client.data.mapper

import com.zaneschepke.wireguardautotunnel.client.data.entity.ProxySettings as Entity
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField
import com.zaneschepke.wireguardautotunnel.client.domain.model.ProxySettings as Domain

fun Entity.toDomain(): Domain =
    Domain(
        id = id,
        socks5ProxyEnabled = socks5ProxyEnabled,
        socks5ProxyBindAddress = socks5ProxyBindAddress,
        httpProxyEnabled = httpProxyEnabled,
        httpProxyBindAddress = httpProxyBindAddress,
        proxyUsername = proxyUsername?.value,
        proxyPassword = proxyPassword?.value,
    )

fun Domain.toEntity(): Entity =
    Entity(
        id = id,
        socks5ProxyEnabled = socks5ProxyEnabled,
        socks5ProxyBindAddress = socks5ProxyBindAddress,
        httpProxyEnabled = httpProxyEnabled,
        httpProxyBindAddress = httpProxyBindAddress,
        proxyUsername = proxyUsername?.let { EncryptedField(it) },
        proxyPassword = proxyPassword?.let { EncryptedField(it) },
    )

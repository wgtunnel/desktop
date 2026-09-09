package com.zaneschepke.wireguardautotunnel.client.domain.error

sealed class ClientException : Exception() {
    class BadRequestException(override val message: String) : ClientException()

    class ConflictException(override val message: String) : ClientException()

    class InternalServerError(override val message: String) : ClientException()

    class UnknownError(override val message: String) : ClientException()

    class UnauthorizedException(override val message: String) : ClientException()

    class DaemonCommsException : ClientException()

    /** Proxy mode was started with both the SOCKS5 and HTTP proxies disabled. */
    class ProxyBothDisabledException : ClientException()
}

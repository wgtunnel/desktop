package com.zaneschepke.wireguardautotunnel.client.domain.error

sealed class ClientException : Exception() {
    class BadRequestException(override val message: String) : ClientException()

    class ConflictException(override val message: String) : ClientException()

    class InternalServerError(override val message: String) : ClientException()

    class UnknownError(override val message: String) : ClientException()

    class DaemonCommsException : ClientException()
}

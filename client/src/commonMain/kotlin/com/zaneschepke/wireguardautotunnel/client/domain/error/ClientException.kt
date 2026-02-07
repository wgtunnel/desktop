package com.zaneschepke.wireguardautotunnel.client.domain.error

sealed class ClientException : Exception() {
    class BackendException(val backendError: BackendError) : ClientException()
    class DaemonCommsException : ClientException()
}
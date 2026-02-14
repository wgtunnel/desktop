package com.zaneschepke.wireguardautotunnel.tunnel.util

sealed class BackendException : Exception() {
    class StateConflict(override val message: String) : BackendException()

    class InternalError(override val message: String) : BackendException()
}

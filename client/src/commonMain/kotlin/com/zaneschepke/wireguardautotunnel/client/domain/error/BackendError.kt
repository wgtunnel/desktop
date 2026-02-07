package com.zaneschepke.wireguardautotunnel.client.domain.error

sealed interface BackendError {
    object AlreadyActive : BackendError
    object NotActive : BackendError
    data class GeneralError(val message: String) : BackendError

    sealed interface StartTunnel : BackendError {
        data object NotFound : StartTunnel
    }
}
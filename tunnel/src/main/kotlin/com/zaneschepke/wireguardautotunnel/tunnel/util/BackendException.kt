package com.zaneschepke.wireguardautotunnel.tunnel.util

sealed class BackendException : Exception() {
    data class InvalidConfig(val reason: String) : BackendException()
    data class PermissionDenied(val requiredPermission: String) : BackendException()
    data class KillSwitchSetFailed(val reason : String) : BackendException()
    data class BackendFailure(override val cause: Throwable) : BackendException()
}
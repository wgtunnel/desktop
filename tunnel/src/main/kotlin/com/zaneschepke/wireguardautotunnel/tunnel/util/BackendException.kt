package com.zaneschepke.wireguardautotunnel.tunnel.util


sealed class BackendException : Exception() {

    data class PermissionDenied(val permission: String) : BackendException()
    class TunnelAlreadyActive : BackendException()
    class TunnelNotActive : BackendException()
    class KillSwitchSetFailed : BackendException()
    class KillSwitchAlreadyActivate : BackendException()
    class TunnelStartFailed : BackendException()
}
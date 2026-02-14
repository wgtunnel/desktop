package com.zaneschepke.wireguardautotunnel.core.ipc

object Routes {
    const val DAEMON_BASE = "/daemon"
    const val DAEMON_STATUS = "$DAEMON_BASE/status"
    const val DAEMON_STATUS_WS = "$DAEMON_BASE/status/ws"

    const val BACKEND_BASE = "/backend"
    const val BACKEND_STATUS = "$BACKEND_BASE/status"

    const val BACKEND_ACTIVE_CONFIG = "$BACKEND_BASE/config/{id}/active"
    const val BACKEND_STATUS_WS = "$BACKEND_BASE/status/ws"
    const val BACKEND_KILL_SWITCH = "$BACKEND_BASE/kill-switch"
    const val BACKEND_MODE = "$BACKEND_BASE/mode"

    object Tunnels {
        private const val BASE = "/tunnel"

        // for server
        const val START_TEMPLATE = "$BASE/{id}/start"
        const val STOP_TEMPLATE = "$BASE/{id}/stop"

        fun start(id: Long) = "$BASE/$id/start"

        fun stop(id: Long) = "$BASE/$id/stop"
    }
}

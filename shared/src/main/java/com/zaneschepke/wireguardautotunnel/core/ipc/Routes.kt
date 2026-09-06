package com.zaneschepke.wireguardautotunnel.core.ipc

object Routes {
    const val DAEMON_BASE = "/daemon"
    const val DAEMON_STATUS = "$DAEMON_BASE/status"
    const val DAEMON_STATUS_WS = "$DAEMON_BASE/status/ws"

    const val DAEMON_RESTORE_TUNNEL = "$DAEMON_BASE/restore/tunnel"
    const val DAEMON_RESTORE_KILL_SWITCH = "$DAEMON_BASE/restore/kill-switch"

    const val DAEMON_AUTO_TUNNEL_PLAN = "$DAEMON_BASE/auto-tunnel/plan"
    const val DAEMON_AUTO_TUNNEL_STATUS = "$DAEMON_BASE/auto-tunnel/status"
    const val DAEMON_AUTO_TUNNEL_STATUS_WS = "$DAEMON_BASE/auto-tunnel/status/ws"
    const val DAEMON_AUTO_TUNNEL_OVERRIDE = "$DAEMON_BASE/auto-tunnel/user-override"

    const val DAEMON_LOGS_ENABLED = "$DAEMON_BASE/logs/enabled"
    const val DAEMON_LOGS_WS = "$DAEMON_BASE/logs/ws"
    const val DAEMON_LOGS_CLEAR = "$DAEMON_BASE/logs/clear"
    const val DAEMON_LOGS_ZIP = "$DAEMON_BASE/logs/zip"

    const val BACKEND_BASE = "/backend"
    const val BACKEND_STATUS = "$BACKEND_BASE/status"
    const val BACKEND_STATUS_WS = "$BACKEND_BASE/status/ws"
    const val BACKEND_KILL_SWITCH = "$BACKEND_BASE/kill-switch"

    object Tunnels {
        private const val BASE = "/tunnel"

        // for server
        const val START_TEMPLATE = "$BASE/{id}/start"
        const val STOP_TEMPLATE = "$BASE/{id}/stop"

        fun start(id: Long) = "$BASE/$id/start"

        fun stop(id: Long) = "$BASE/$id/stop"
    }
}

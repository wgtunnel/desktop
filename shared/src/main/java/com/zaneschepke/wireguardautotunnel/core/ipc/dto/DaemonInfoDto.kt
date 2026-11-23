package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

/**
 * [canRestart] is true when a service manager relaunches the daemon after it exits, so a client can
 * ask it to restart to a newly installed binary. Earlier daemons report false.
 */
@Serializable data class DaemonInfoDto(val version: String, val canRestart: Boolean = false)

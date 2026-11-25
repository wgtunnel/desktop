package com.zaneschepke.wireguardautotunnel.daemon.tunnel

import com.wgtunnel.backend.Backend
import com.wgtunnel.parser.Config
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository
import com.zaneschepke.wireguardautotunnel.daemon.dto.applyForBootstrap
import com.zaneschepke.wireguardautotunnel.daemon.dto.outerConfigOrNull
import com.zaneschepke.wireguardautotunnel.daemon.dto.toBackendMode
import com.zaneschepke.wireguardautotunnel.daemon.dto.toCore

/** Starts a tunnel from a client's request, as the start route does. */
suspend fun Backend.startFromRequest(
    id: Int,
    request: StartTunnelRequest,
    cacheRepository: DaemonCacheRepository,
): Result<Unit> {
    val config = runCatching {
        Config.parseQuickString(request.quickConfig)
    }
        .getOrElse {
            return Result.failure(it)
        }
    val outerConfig = runCatching {
        request.outerConfigOrNull()
    }
        .getOrElse {
            return Result.failure(it)
        }
    // Peer resolution reads the live mode. Apply this request's mode before start.
    request.bootstrapDns?.applyForBootstrap(this, cacheRepository)
    return start(
        tunnel = RunningTunnel.fromRequest(id, request),
        mode = request.toBackendMode(config),
        tunnelDnsConfig = request.tunnelDns?.toCore(),
        outerConfig = outerConfig,
    )
}

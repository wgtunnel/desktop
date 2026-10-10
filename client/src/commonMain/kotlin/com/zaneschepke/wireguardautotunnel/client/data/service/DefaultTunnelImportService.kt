package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelGroupRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.extensions.saveTunnelsUniquely
import com.zaneschepke.wireguardautotunnel.client.service.QuickConfigMap
import com.zaneschepke.wireguardautotunnel.client.service.QuickString
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelName

class DefaultTunnelImportService(
    private val tunnelRepository: TunnelRepository,
    private val tunnelGroupRepository: TunnelGroupRepository,
) : TunnelImportService {

    private val log = Logger.withTag("TunnelImport")

    override suspend fun import(config: QuickString, name: TunnelName?): Result<Unit> =
        import(mapOf(config to name))

    override suspend fun import(configs: QuickConfigMap): Result<Unit> = runCatching {
        val tunnelConfigs = configs.map { (config, name) ->
            TunnelConfig.fromQuickString(config, name)
        }
        if (tunnelConfigs.isNotEmpty()) {
            val groups = tunnelGroupRepository.getAll()
            val tunnels = tunnelRepository.getAll()
            // New tunnels land at the top
            val count = tunnelConfigs.size
            val shiftedGroups = groups.map { it.copy(position = it.position + count) }
            val shiftedTunnels = tunnels.map {
                if (it.groupId == null) it.copy(position = it.position + count) else it
            }
            tunnelGroupRepository.saveAll(shiftedGroups)
            tunnelRepository.saveAll(shiftedTunnels)
            val positioned = tunnelConfigs.mapIndexed { index, tunnel ->
                tunnel.copy(position = index)
            }
            tunnelRepository.saveTunnelsUniquely(positioned, tunnels.map { it.name })
        }
    }
        .onFailure { log.e(it) { "Tunnel import failed" } }
}

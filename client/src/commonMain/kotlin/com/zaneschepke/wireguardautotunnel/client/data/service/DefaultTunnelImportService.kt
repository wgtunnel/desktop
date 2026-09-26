package com.zaneschepke.wireguardautotunnel.client.data.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.extensions.saveTunnelsUniquely
import com.zaneschepke.wireguardautotunnel.client.service.QuickConfigMap
import com.zaneschepke.wireguardautotunnel.client.service.QuickString
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelName

class DefaultTunnelImportService(private val tunnelRepository: TunnelRepository) :
    TunnelImportService {

    private val log = Logger.withTag("TunnelImport")

    override suspend fun import(config: QuickString, name: TunnelName?): Result<Unit> =
        import(mapOf(config to name))

    override suspend fun import(configs: QuickConfigMap): Result<Unit> = runCatching {
        val tunnelConfigs = configs.map { (config, name) ->
            TunnelConfig.fromQuickString(config, name)
        }
        if (tunnelConfigs.isNotEmpty()) {
            val existingNames = tunnelRepository.getAll().map { it.name }
            tunnelRepository.saveTunnelsUniquely(tunnelConfigs, existingNames)
        }
    }
        .onFailure { log.e(it) { "Tunnel import failed" } }
}

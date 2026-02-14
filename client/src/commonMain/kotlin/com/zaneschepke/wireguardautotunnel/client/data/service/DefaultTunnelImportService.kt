package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.extensions.saveTunnelsUniquely
import com.zaneschepke.wireguardautotunnel.client.service.QuickConfigMap
import com.zaneschepke.wireguardautotunnel.client.service.QuickString
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelName

class DefaultTunnelImportService(private val tunnelRepository: TunnelRepository) :
    TunnelImportService {

    override suspend fun import(config: QuickString, name: TunnelName?): Result<Unit> =
        runCatching {
            import(mapOf(config to name))
        }

    override suspend fun import(configs: QuickConfigMap): Result<Unit> = runCatching {
        val tunnelConfigs =
            configs.mapNotNull { (config, name) ->
                try {
                    val tunnel = TunnelConfig.fromQuickString(config, name)
                    tunnel
                } catch (_: Exception) {
                    null
                }
            }
        if (tunnelConfigs.isNotEmpty()) {
            val existingNames = tunnelRepository.getAll().map { it.name }
            tunnelRepository.saveTunnelsUniquely(tunnelConfigs, existingNames)
        }
    }
}

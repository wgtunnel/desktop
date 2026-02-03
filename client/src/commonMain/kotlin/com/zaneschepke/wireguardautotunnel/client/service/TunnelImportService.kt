package com.zaneschepke.wireguardautotunnel.client.service


typealias QuickString = String
typealias TunnelName = String
typealias QuickConfigMap = Map<QuickString, TunnelName?>

interface TunnelImportService {
    suspend fun import(config: QuickString, name: TunnelName? = null)
    suspend fun import(configs: QuickConfigMap)
}
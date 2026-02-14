package com.zaneschepke.wireguardautotunnel.client.service

import kotlinx.coroutines.flow.Flow

interface DaemonHealthService {
    suspend fun alive(): Boolean

    val alive: Flow<Boolean>
}

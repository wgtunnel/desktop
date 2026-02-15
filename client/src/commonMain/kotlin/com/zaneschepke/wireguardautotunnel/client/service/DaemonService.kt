package com.zaneschepke.wireguardautotunnel.client.service

import kotlinx.coroutines.flow.Flow

interface DaemonService {
    suspend fun alive(): Boolean

    suspend fun setRestoreKillSwitch(enabled: Boolean): Result<Unit>

    suspend fun setRestoreTunnel(enabled: Boolean): Result<Unit>

    val alive: Flow<Boolean>
}

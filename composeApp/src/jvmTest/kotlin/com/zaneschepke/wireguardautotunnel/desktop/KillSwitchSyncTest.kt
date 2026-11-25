package com.zaneschepke.wireguardautotunnel.desktop

import com.zaneschepke.wireguardautotunnel.client.domain.model.LockdownSettings
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.orchestration.KillSwitchSync
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.BootstrapDnsModeRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking

class KillSwitchSyncTest {

    private class FakeBackend(var status: Result<BackendStatus>) : BackendService {
        override suspend fun setKillSwitch(enabled: Boolean, config: KillSwitchConfigDto?) =
            error("sync must not change the daemon")

        override suspend fun setBootstrapDnsMode(mode: BootstrapDnsModeRequest) =
            Result.success(Unit)

        override suspend fun getStatus() = status

        override fun statusFlow(): Flow<BackendStatus> = emptyFlow()
    }

    private class FakeLockdown(enabled: Boolean) : LockdownSettingsRepository {
        override val flow = MutableStateFlow(LockdownSettings(enabled = enabled, bypassLan = true))
        var writes = 0

        override suspend fun upsert(lockdownSettings: LockdownSettings) {
            flow.value = lockdownSettings
        }

        override suspend fun updateEnabled(enabled: Boolean) {
            writes++
            flow.value = flow.value.copy(enabled = enabled)
        }

        override suspend fun updateBypassLan(enabled: Boolean) {
            flow.value = flow.value.copy(bypassLan = enabled)
        }

        override suspend fun updateRestoreOnBoot(enabled: Boolean) {
            flow.value = flow.value.copy(restoreOnBoot = enabled)
        }

        override suspend fun get() = flow.value
    }

    private fun sync(appEnabled: Boolean, daemon: Result<BackendStatus>) =
        FakeLockdown(appEnabled).let { lockdown ->
            lockdown to
                runBlocking { KillSwitchSync(FakeBackend(daemon), lockdown).syncFromDaemon() }
        }

    @Test
    fun `turns the toggle off when the daemon no longer enforces it`() {
        val (lockdown, result) =
            sync(true, Result.success(BackendStatus(killSwitchEnabled = false)))
        assertTrue(result.getOrThrow())
        assertFalse(lockdown.flow.value.enabled)
        // The rest of the config stays the app's.
        assertTrue(lockdown.flow.value.bypassLan)
    }

    @Test
    fun `turns the toggle on when the daemon enforces it`() {
        val (lockdown, result) =
            sync(false, Result.success(BackendStatus(killSwitchEnabled = true)))
        assertTrue(result.getOrThrow())
        assertTrue(lockdown.flow.value.enabled)
    }

    @Test
    fun `leaves a matching toggle alone`() {
        val (lockdown, result) = sync(true, Result.success(BackendStatus(killSwitchEnabled = true)))
        assertFalse(result.getOrThrow())
        assertEquals(0, lockdown.writes)
    }

    @Test
    fun `keeps the toggle when the daemon status can't be read`() {
        val (lockdown, result) = sync(true, Result.failure(IllegalStateException("down")))
        assertTrue(result.isFailure)
        assertTrue(lockdown.flow.value.enabled)
        assertEquals(0, lockdown.writes)
    }
}

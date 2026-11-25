package com.zaneschepke.wireguardautotunnel.client.orchestration

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.LockdownSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.service.BackendService

/**
 * Matches the app's kill switch toggle to what the daemon enforces, keeping daemon as the source of
 * truth. Runs once per connection, not on every status, so a status sent before a toggle the user
 * just made can't undo it.
 */
class KillSwitchSync(
    private val backendService: BackendService,
    private val lockdownRepository: LockdownSettingsRepository,
) {
    private val log = Logger.withTag("KillSwitchSync")

    /** @return true when the app's setting changed. */
    suspend fun syncFromDaemon(): Result<Boolean> = runCatching {
        val enforced = backendService.getStatus().getOrThrow().killSwitchEnabled
        if (lockdownRepository.get().enabled == enforced) return@runCatching false
        log.i { "Kill switch is ${if (enforced) "on" else "off"} in the daemon, matching it" }
        lockdownRepository.updateEnabled(enforced)
        true
    }
}

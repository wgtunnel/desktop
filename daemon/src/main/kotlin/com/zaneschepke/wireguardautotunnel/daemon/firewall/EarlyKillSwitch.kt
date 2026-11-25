package com.zaneschepke.wireguardautotunnel.daemon.firewall

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.system.DesktopFirewall
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.KillSwitchConfigDto
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonCacheRepository

/**
 * Puts the kill switch back as early as possible as the daemon's first step, and before networking
 * from the Linux early boot unit. Needs only the cache and the firewall.
 */
object EarlyKillSwitch {
    private val log = Logger.withTag("EarlyKillSwitch")

    /**
     * Rules left by a daemon that exited earlier in this boot mean the kill switch was on, so they
     * are taken over without a gap. On Windows, persistent rules kept across a reboot count as
     * well. Otherwise, restore on boot decides.
     *
     * @return the config now enforced, or null when the kill switch should stay off.
     */
    suspend fun apply(cache: DaemonCacheRepository): KillSwitchConfigDto? {
        val leftover = DesktopFirewall.hasLeftoverRules()
        val enabled = cache.getKillSwitchEnabled()
        val config = cache.getKillSwitchConfig()
        val restore =
            when {
                leftover && enabled && config != null -> {
                    log.i { "Taking over the kill switch a previous run left enforced" }
                    true
                }
                leftover -> {
                    log.i { "Removing kill switch rules a previous run left" }
                    if (!DesktopFirewall.purge()) log.e { "Failed to remove leftover rules" }
                    false
                }
                enabled && config != null && cache.getKillSwitchRestore() -> {
                    log.i { "Restoring kill switch on boot" }
                    true
                }
                else -> false
            }
        if (!restore || config == null) return null
        // On failure, leftover rules stay enforced, so this fails closed.
        if (!DesktopFirewall.enforce(config.allowedIps)) log.e { "Failed to enforce kill switch" }
        return config
    }
}

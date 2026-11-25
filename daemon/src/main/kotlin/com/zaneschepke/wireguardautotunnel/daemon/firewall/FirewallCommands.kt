package com.zaneschepke.wireguardautotunnel.daemon.firewall

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.system.DesktopFirewall
import com.zaneschepke.wireguardautotunnel.daemon.data.DaemonSessionStore
import com.zaneschepke.wireguardautotunnel.daemon.data.SettingsDaemonCacheRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/** One-shot kill switch commands the daemon binary runs instead of starting the daemon. */
object FirewallCommands {
    private val log = Logger.withTag("FirewallCommands")

    private const val PURGE_ARG = "--purge-firewall"
    private const val EARLY_BOOT_ARG = "--early-boot-firewall"

    /** Runs the command in [args] and returns its exit code, or null when there is none. */
    fun run(args: Array<String>, json: Json): Int? =
        when {
            PURGE_ARG in args -> purge(json)
            EARLY_BOOT_ARG in args -> earlyBoot(json)
            else -> null
        }

    /**
     * Run by the Linux early boot unit before networking. Enforces the kill switch when it should
     * be on and leaves it in place for the daemon to take over.
     */
    private fun earlyBoot(json: Json): Int {
        val config = runBlocking { EarlyKillSwitch.apply(SettingsDaemonCacheRepository(json)) }
        if (config != null && !DesktopFirewall.release()) return 1
        log.i { "Early boot kill switch enforced=${config != null}" }
        return 0
    }

    /**
     * Removes every kill switch rule and turns the kill switch off, so a restarted daemon doesn't
     * put it back. Run as root/SYSTEM by uninstallers, and by the app's "Remove block" when the
     * daemon can't run.
     */
    private fun purge(json: Json): Int {
        val purged = DesktopFirewall.purge()
        runBlocking { SettingsDaemonCacheRepository(json).updateKillSwitchEnabled(false) }
        DaemonSessionStore(json).clear()
        log.i { "Kill switch rules removed=$purged" }
        return if (purged) 0 else 1
    }
}

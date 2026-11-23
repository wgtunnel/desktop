package com.zaneschepke.wireguardautotunnel.desktop.service

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import dev.nucleusframework.servicemanagement.AppService
import dev.nucleusframework.servicemanagement.AppServiceManager
import dev.nucleusframework.servicemanagement.AppServiceStatus
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The daemon and Wi-Fi helper SMAppService registrations. Calls for the two must run on different
 * coroutines, a second call on the same one hangs.
 */
object MacBackgroundServices {
    private val log = Logger.withTag("MacBackgroundServices")

    val daemon = AppService.Daemon("${AppVariant.current.linuxFsName}-daemon")
    val wifiAgent = AppService.Agent("${AppVariant.current.linuxFsName}-wifi")

    private fun nameOf(service: AppService) =
        if (service is AppService.Daemon) "daemon" else "Wi-Fi helper"

    fun status(service: AppService): AppServiceStatus = AppServiceManager.status(service)

    fun register(service: AppService): Result<Unit> = AppServiceManager.register(service)

    /** Null on success, or when [service] was not registered. */
    suspend fun unregister(service: AppService): String? {
        val status = status(service)
        if (status == AppServiceStatus.NOT_REGISTERED || status == AppServiceStatus.NOT_FOUND) {
            return null
        }
        return suspendCancellableCoroutine { cont ->
            AppServiceManager.unregister(service) { cont.resume(it) }
        }
    }

    /** Makes launchd drop the running process and load the binary now in the bundle. */
    suspend fun reregister(service: AppService): Boolean {
        unregister(service)?.let {
            log.w { "Failed to unregister the ${nameOf(service)}: $it" }
            return false
        }
        return register(service)
            .onFailure { log.w(it) { "Failed to register the ${nameOf(service)}" } }
            .isSuccess
    }
}

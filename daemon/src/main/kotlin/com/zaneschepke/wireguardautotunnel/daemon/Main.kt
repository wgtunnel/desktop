package com.zaneschepke.wireguardautotunnel.daemon

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.daemon.di.daemonModule
import com.zaneschepke.wireguardautotunnel.daemon.util.initLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import kotlin.system.exitProcess

fun main() {
    initLogger()
    startKoin {
        modules(daemonModule)
    }

    val daemon : TunnelDaemon by inject(TunnelDaemon::class.java)

    try {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                Logger.i { "Stopping daemon..." }
                daemon.stop()
            }
        )

        Logger.i  { "Starting daemon..." }
        daemon.run()
    } catch (e: Exception) {
        Logger.e(e) { "Shutting down..." }
        exitProcess(1)
    }
}
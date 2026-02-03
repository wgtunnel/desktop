package com.zaneschepke.wireguardautotunnel.daemon.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

fun initLogger() {
    Logger.setLogWriters(platformLogWriter())
    Logger.setMinSeverity(Severity.Debug)
    Logger.setTag("Daemon")
}
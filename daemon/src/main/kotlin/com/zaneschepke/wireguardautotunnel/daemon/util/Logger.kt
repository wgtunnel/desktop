package com.zaneschepke.wireguardautotunnel.daemon.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.wgtunnel.backend.BackendLog
import com.wgtunnel.backend.LogLevel
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant

fun initLogger() {
    Logger.setLogWriters(platformLogWriter())
    Logger.setTag("Daemon")
    BackendLog.setMinLevel(
        if (AppVariant.current == AppVariant.DEBUG) LogLevel.Debug else LogLevel.Info
    )
}

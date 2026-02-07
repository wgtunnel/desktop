package com.zaneschepke.wireguardautotunnel.cli

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.zaneschepke.wireguardautotunnel.cli.handler.CliExceptionHandler
import com.zaneschepke.wireguardautotunnel.cli.strategy.CliExecutionStrategy
import com.zaneschepke.wireguardautotunnel.client.di.databaseModule
import com.zaneschepke.wireguardautotunnel.client.di.serviceModule
import org.koin.core.context.startKoin
import picocli.CommandLine

fun main(args: Array<String>) {

    Logger.setLogWriters(platformLogWriter())
    Logger.setMinSeverity(Severity.Debug)
    Logger.setTag("CLI")

    val koinApp = startKoin {
        modules(databaseModule, serviceModule)
    }

    try {
        val commandLine = CommandLine(CliRoot::class.java)
        commandLine.executionStrategy = CliExecutionStrategy(commandLine.executionStrategy)
        commandLine.executionExceptionHandler = CliExceptionHandler()

        commandLine.execute(*args)
    } finally {
        koinApp.close()
    }
}
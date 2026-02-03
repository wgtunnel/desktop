package com.zaneschepke.wireguardautotunnel.cli.strategy

import com.zaneschepke.wireguardautotunnel.client.service.DaemonHealthService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*

class CliExecutionStrategy(private val defaultStrategy: IExecutionStrategy) : IExecutionStrategy {

    val daemonHealthService : DaemonHealthService by inject(DaemonHealthService::class.java)

    override fun execute(parseResult: ParseResult): Int = runBlocking {
        // Drill down to the deepest subcommand
        var current = parseResult
        while (current.hasSubcommand()) {
            current = current.subcommand()
        }
        val commandSpec = current.commandSpec()

        val skipCheck = parseResult.isUsageHelpRequested || parseResult.isVersionHelpRequested

//        if (!skipCheck && !daemonHealthService.alive()) {
//            throw ExecutionException(
//                commandSpec.commandLine(),
//                "The WG Tunnel service must be installed and started to execute this command. " +
//                        "Install and start it with 'wgtunnel service install -y' or, if already installed, " +
//                        "start the service with 'wgtunnel service start'."
//            )
//        }

        return@runBlocking defaultStrategy.execute(parseResult)
    }
}
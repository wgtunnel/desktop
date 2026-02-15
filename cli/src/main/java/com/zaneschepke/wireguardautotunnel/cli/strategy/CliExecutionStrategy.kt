package com.zaneschepke.wireguardautotunnel.cli.strategy

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*

class CliExecutionStrategy(private val defaultStrategy: IExecutionStrategy) : IExecutionStrategy {

    private val daemonService: DaemonService by inject(DaemonService::class.java)

    override fun execute(parseResult: ParseResult): Int = runBlocking {
        // skip help and version
        if (parseResult.isUsageHelpRequested || parseResult.isVersionHelpRequested) {
            return@runBlocking defaultStrategy.execute(parseResult)
        }

        val isAlive =
            try {
                daemonService.alive()
            } catch (e: Exception) {
                false
            }

        if (!isAlive) {
            CliUtils.printError("WG Tunnel daemon is not reachable.")
            println("Please ensure the service is active and the socket is available.")
            return@runBlocking 1 // exit
        }
        // proceed
        defaultStrategy.execute(parseResult)
    }
}

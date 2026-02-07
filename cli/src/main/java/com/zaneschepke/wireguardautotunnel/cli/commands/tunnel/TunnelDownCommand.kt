package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.client.domain.error.BackendError
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable

@Command(name = "down", description = ["Bring a tunnel down."])
class TunnelDownCommand : Callable<Int> {
    private val tunnelService: TunnelCommandService by inject(TunnelCommandService::class.java)
    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["The name of the tunnel to bring down."])
    lateinit var tunnelName: String

    override fun call(): Int = runBlocking {
        val tunnel = tunnelRepository.getTunnelByName(tunnelName) ?: run {
            CliUtils.printError("Tunnel '$tunnelName' not found.")
            return@runBlocking 1
        }

        val result = CliUtils.withSpinner("Stopping tunnel '$tunnelName'...") {
            tunnelService.stopTunnel(tunnel.id)
        }

        result.fold(
            onSuccess = {
                CliUtils.printSuccess("Tunnel '$tunnelName' has been stopped.")
                0
            },
            onFailure = { error ->
                val message = mapErrorToMessage(error)
                CliUtils.printError(message)
                1
            }
        )
    }

    private fun mapErrorToMessage(error: Throwable): String {
        return when (error) {
            is ClientException.DaemonCommsException ->
                "Could not communicate with the WireGuard daemon."

            is ClientException.BackendException -> {
                when (val backendError = error.backendError) {
                    is BackendError.NotActive -> "Tunnel '$tunnelName' is not currently active."
                    is BackendError.GeneralError -> "Failed with error: ${backendError.message}"
                    else -> "Daemon error: ${backendError::class.simpleName}"
                }
            }

            else -> error.message ?: "An unexpected error occurred: ${error::class.simpleName}"
        }
    }
}
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

@Command(name = "up", description = ["Bring a tunnel up."])
class TunnelUpCommand : Callable<Int> {
    private val tunnelService: TunnelCommandService by inject(TunnelCommandService::class.java)
    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["The name of the tunnel to bring up."])
    lateinit var tunnelName: String

    override fun call(): Int = runBlocking {

        val tunnel = tunnelRepository.getTunnelByName(tunnelName) ?: run {
            CliUtils.printError("Tunnel '$tunnelName' not found.")
            return@runBlocking 1
        }

        val result = CliUtils.withSpinner("Starting tunnel '$tunnelName'...") {
            tunnelService.startTunnel(tunnel.id)
        }

        result.fold(
            onSuccess = {
                CliUtils.printSuccess("Tunnel '$tunnelName' is now active.")
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
            is ClientException.DaemonCommsException -> "Cannot reach the WireGuard daemon. Is it running?"
            is ClientException.BackendException -> when (error.backendError) {
                is BackendError.AlreadyActive -> "Tunnel is already running."
                is BackendError.StartTunnel.NotFound -> "The daemon could not find the configuration for '$tunnelName'."
                is BackendError.GeneralError -> {
                    val generalError = error.backendError as BackendError.GeneralError
                    "Failed to start tunnel with general error: ${generalError.message}"
                }
                else -> "Daemon error: ${error.backendError::class.java.name}"
            }
            else -> error.message ?: "An unexpected error occurred."
        }
    }
}
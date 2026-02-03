package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "up", description = ["Bring a tunnel up."])
class TunnelUpCommand : Runnable {
    private val tunnelService: TunnelCommandService by inject(TunnelCommandService::class.java)
    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["The name of the tunnel to bring up."])
    lateinit var tunnelName: String

    override fun run() {
        runBlocking {
            val tunnel = tunnelRepository.getTunnelByName(tunnelName) ?: return@runBlocking println("Failed to find the $tunnelName")
            val result = tunnelService.startTunnel(tunnel.id)
            if (result.isSuccess) {
                println("Tunnel start triggered successfully.")
            } else {
                println("Failed to start tunnel: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }
}
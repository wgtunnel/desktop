package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "down", description = ["Bring a tunnel down."])
class TunnelDownCommand : Runnable {
    private val tunnelService: TunnelCommandService by inject(TunnelCommandService::class.java)

    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["The name of the tunnel to bring down."])
    lateinit var tunnelName: String

    override fun run() {
        runBlocking {
            val tunnel = tunnelRepository.getTunnelByName(tunnelName) ?: return@runBlocking println("Tunnel $tunnelName not found")
            val result = tunnelService.stopTunnel(tunnel.id)
            if (result.isSuccess) {
                println("Tunnel stopped successfully.")
            } else {
                println("Failed to stop tunnel: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }
}
package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*
import java.util.concurrent.Callable

@Command(
    name = "delete",
    description = ["Delete a tunnel."],
)
class TunnelDeleteCommand : Callable<Int> {

    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Option(names = ["-y", "--yes"], description = ["Delete without additional prompts."])
    var yes: Boolean? = null

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["The name of the tunnel to bring up."])
    lateinit var tunnelName: String

    override fun call(): Int = runBlocking {
        if(yes == null) {
            print("Are you sure you want to delete $tunnelName? [y/N]: ")
            val userInput = readlnOrNull()?.trim()?.lowercase()
            if (userInput != "y" && userInput != "yes") return@runBlocking 0
        }
        try {
            tunnelRepository.deleteByName(tunnelName)
        } catch (_: Exception) {
            System.err.println("Failed to delete $tunnelName! Check that the service is running.")
            return@runBlocking 1
        }

        return@runBlocking 0
    }
}
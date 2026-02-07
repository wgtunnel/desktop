package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*
import java.util.concurrent.Callable

@Command(
    name = "delete",
    description = ["Delete a tunnel config."],
    mixinStandardHelpOptions = true
)
class TunnelDeleteCommand : Callable<Int> {

    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Option(names = ["-y", "--yes"], description = ["Confirm deletion without prompting."])
    var force: Boolean = false

    @Parameters(index = "0", paramLabel = "<tunnel-name>", description = ["Name of the tunnel to delete."])
    lateinit var tunnelName: String

    override fun call(): Int = runBlocking {
        val tunnel = tunnelRepository.getTunnelByName(tunnelName) ?: run {
            CliUtils.printError("Tunnel '$tunnelName' not found.")
            return@runBlocking 1
        }

        if (!force && !CliUtils.confirm("Are you sure you want to delete '$tunnelName'?")) {
            CliUtils.printInfo("Delete cancelled.")
            return@runBlocking 0
        }

        CliUtils.withSpinner("Deleting '$tunnelName'...") {
            tunnelRepository.delete(tunnel)
        }

        CliUtils.printSuccess("Tunnel '$tunnelName' deleted successfully.")
        0
    }
}
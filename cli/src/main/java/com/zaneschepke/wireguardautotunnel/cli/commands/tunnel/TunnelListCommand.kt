package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils.renderAnsi
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(
    name = "list",
    description = ["List of tunnels."],
    mixinStandardHelpOptions = true
)
class TunnelListCommand : Callable<Int> {

    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Option(names = ["--json"], descriptionKey = "Output in JSON")
    var json: Boolean = false

    override fun call(): Int = runBlocking {
        val tunnels = try {
            tunnelRepository.getAll().sortedBy { it.position }
        } catch (e: Exception) {
            CliUtils.printError("Failed to retrieve tunnels: ${e.message}")
            return@runBlocking 1
        }

        if (tunnels.isEmpty()) {
            CliUtils.printInfo("No tunnels found.")
            return@runBlocking 0
        }

        if (json) {
            println(Json.encodeToString(tunnels))
        } else {
            renderSimpleList(tunnels)
        }

        0
    }

    private fun renderSimpleList(tunnels: List<TunnelConfig>) {
        println("@|bold,underline Configured Tunnels:|@".renderAnsi())

        tunnels.forEach { tunnel ->
            println("@|faint  ● |@ ${tunnel.name}".renderAnsi())
        }

        println("Total: ${tunnels.size}".renderAnsi())
    }
}

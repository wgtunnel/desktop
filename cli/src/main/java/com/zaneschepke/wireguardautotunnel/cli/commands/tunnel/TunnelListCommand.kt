package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(
    name = "list",
    description = ["List configured WG Tunnel tunnels."]
)
class TunnelListCommand : Callable<Int> {

    private val tunnelRepository: TunnelRepository by inject(TunnelRepository::class.java)

    @Option(names = ["--json"], description = ["Output in JSON format for scripting."])
    var json: Boolean = false

    override fun call(): Int = runBlocking {
        val tunnels = try {
            tunnelRepository.getAll().sortedBy { it.position }
        } catch (e: Exception) {
            Logger.e("failed to load tunnels", e)
            System.err.println("Error: Failed to retrieve tunnels. ${e.message}")
            return@runBlocking 1
        }

        if (tunnels.isEmpty()) {
            println("No tunnels found")
            return@runBlocking 0
        }

        if (json) {
            val names = tunnels.map { it.name }
            println(Json.encodeToString(names))
        } else {
            // TODO better strategy for large number of tunnels
            println("Configured Tunnels:")
            tunnels.forEach { println(it.name) }
        }

        return@runBlocking 0
    }
}

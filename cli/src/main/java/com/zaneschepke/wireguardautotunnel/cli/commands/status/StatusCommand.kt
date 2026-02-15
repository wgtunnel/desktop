package com.zaneschepke.wireguardautotunnel.cli.commands.status

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils.renderAnsi
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendMode
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.BackendStatus
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*

@Command(
    name = "status",
    description = ["View backend status and active tunnels."],
    mixinStandardHelpOptions = true,
)
class StatusCommand : Callable<Int> {
    private val backendService: BackendService by inject(BackendService::class.java)

    override fun call(): Int = runBlocking { fetchSnapshot() }

    private suspend fun fetchSnapshot(): Int {
        return backendService
            .getStatus()
            .fold(
                onSuccess = {
                    renderStatus(it)
                    0
                },
                onFailure = {
                    CliUtils.printError("Error: ${it.message}")
                    1
                },
            )
    }

    private fun renderStatus(status: BackendStatus, isLive: Boolean = false) {
        val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val clear = "\u001b[K"

        val modeColor = if (status.mode == BackendMode.USERSPACE) "cyan" else "magenta"
        val ksColor = if (status.killSwitchEnabled) "red" else "green"
        val ksText = if (status.killSwitchEnabled) "ENABLED" else "DISABLED"

        val output = buildString {
            append("@|faint [$time]|@ @|bold Mode:|@ @|$modeColor ${status.mode}|@$clear\n")
            append("@|faint [$time]|@ @|bold Kill Switch:|@ @|$ksColor $ksText|@$clear\n")

            if (status.activeTunnels.isEmpty()) {
                append("@|faint No active tunnels.|@$clear\n")
            } else {
                append("@|bold Active Tunnels:|@$clear\n")
                status.activeTunnels.forEach { append("  @|green ●|@ ${it.name}$clear\n") }
            }
        }
        println(output.renderAnsi())
    }
}

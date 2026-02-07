package com.zaneschepke.wireguardautotunnel.cli.commands.killswitch

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils.renderAnsi
import com.zaneschepke.wireguardautotunnel.client.service.BackendCommandService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*
import java.util.concurrent.Callable

@Command(
    name = "killswitch",
    description = ["Enable or disable the global network kill switch."],
    mixinStandardHelpOptions = true
)
class KillSwitchCommand : Callable<Int> {
    private val backendService: BackendCommandService by inject(BackendCommandService::class.java)

    @Parameters(index = "0", description = ["The desired state: 'on' or 'off' (or true/false)."])
    lateinit var state: String

    override fun call(): Int = runBlocking {
        val enabled = when (state.lowercase()) {
            "on", "true", "yes", "1" -> true
            "off", "false", "no", "0" -> false
            else -> {
                CliUtils.printError("Invalid state '$state'. Please use 'on' or 'off'.")
                return@runBlocking 1
            }
        }

        val actionText = if (enabled) "Enabling" else "Disabling"
        val result = CliUtils.withSpinner("$actionText kill switch...") {
            backendService.setKillSwitch(enabled)
        }

        result.fold(
            onSuccess = {
                val status = if (enabled) "@|red,bold ENABLED|@" else "@|green,bold DISABLED|@"
                CliUtils.printSuccess("Kill switch is now ${status.renderAnsi()}.")
                0
            },
            onFailure = { error ->
                CliUtils.printError("Failed to update kill switch: ${error.message}")
                1
            }
        )
    }
}
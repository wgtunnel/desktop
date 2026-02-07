package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import com.zaneschepke.wireguardautotunnel.cli.util.CliUtils
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "import",
    description = ["Import configuration from a file, string, or stdin."]
)
class TunnelImportCommand : Callable<Int> {

    private val tunnelImportService: TunnelImportService by inject(TunnelImportService::class.java)

    @ArgGroup(exclusive = true, multiplicity = "1")
    lateinit var input: Input

    class Input {
        @Option(names = ["-f", "--file"], description = ["Import config from file."])
        var file: File? = null

        @Option(names = ["-s", "--string"], description = ["Import config from string literal."])
        var string: String? = null

        @Option(names = ["-"], description = ["Import config from stdin (standard input)."])
        var stdin: Boolean = false
    }

    @Option(names = ["-n", "--name"], description = ["Specify a custom name for the tunnel."])
    var name: String? = null

    override fun call(): Int = runBlocking {
        val configString = try {
            resolveInput()
        } catch (e: Exception) {
            CliUtils.printError(e.message ?: "Failed to read input source.")
            return@runBlocking 1
        } ?: return@runBlocking 1

        val finalName = name ?: input.file?.nameWithoutExtension ?: "imported-tunnel"

        val result = CliUtils.withSpinner("Importing tunnel '$finalName'...") {
            tunnelImportService.import(configString, finalName)
        }

        result.fold(
            onSuccess = {
                CliUtils.printSuccess("Tunnel '$finalName' imported successfully.")
                0
            },
            onFailure = { error ->
                CliUtils.printError("Import failed: ${error.message}")
                1
            }
        )
    }

    private fun resolveInput(): String? {
        return when {
            input.file != null -> {
                val f = input.file!!
                if (!f.exists() || !f.isFile) {
                    CliUtils.printError("File does not exist or is invalid: ${f.absolutePath}")
                    null
                } else f.readText()
            }
            input.string != null -> input.string!!
            input.stdin -> {
                System.`in`.bufferedReader().readText().ifBlank {
                    CliUtils.printError("Stdin was empty.")
                    null
                }
            }
            else -> null
        }
    }
}

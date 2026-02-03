package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

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
        @Option(names = ["--file"], description = ["Import config from file"])
        var file: File? = null

        @Option(names = ["--string"], description = ["Import config from string literal"])
        var string: String? = null
    }

    @Option(names = ["--name"], description = ["Specify a tunnel name"])
    var name: String? = null

    override fun call(): Int = runBlocking {
        val config : String = try {
            when {
                input.file != null -> {
                    val f = input.file!!
                    if (!f.exists()) {
                        System.err.println("Error: File does not exist: ${f.absolutePath}")
                        return@runBlocking 1
                    }
                    if (!f.isFile) {
                        System.err.println("Error: Not a file: ${f.absolutePath}")
                        return@runBlocking 1
                    }
                    f.readText()
                }

                input.string != null -> input.string!!

                else -> {
                    System.err.println("Error: No input source provided. Use --file, --string, or - for stdin.")
                    return@runBlocking 1
                }
            }
        } catch (e: Exception) {
            System.err.println("Error reading input: ${e.message}")
            return@runBlocking 1
        }

        val name = name ?: input.file?.nameWithoutExtension

        tunnelImportService.import(config , name)

        return@runBlocking 0
    }

}

package com.zaneschepke.wireguardautotunnel.cli.commands.handler

import picocli.CommandLine
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.ParseResult

class CliExceptionHandler : IExecutionExceptionHandler {
    override fun handleExecutionException(
        ex: Exception,
        commandLine: CommandLine,
        parseResult: ParseResult
    ): Int {
        commandLine.err.println(
            commandLine.colorScheme.errorText("Error completing command: ${ex.message}")
        )
        return CommandLine.ExitCode.SOFTWARE
    }
}

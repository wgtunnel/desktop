package com.zaneschepke.wireguardautotunnel.cli.commands.tunnel

import picocli.CommandLine.Command

@Command(
    name = "tunnel",
    mixinStandardHelpOptions = true,
    subcommands =
        [
            TunnelUpCommand::class,
            TunnelDownCommand::class,
            TunnelImportCommand::class,
            TunnelListCommand::class,
            TunnelDeleteCommand::class,
        ],
)
class TunnelCommand : Runnable {
    override fun run() {
        println("Please specify a subcommand: start, stop, list, etc..")
    }
}

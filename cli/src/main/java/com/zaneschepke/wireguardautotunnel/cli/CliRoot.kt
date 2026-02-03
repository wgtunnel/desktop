package com.zaneschepke.wireguardautotunnel.cli

import com.zaneschepke.wireguardautotunnel.cli.CliRoot.Companion.BANNER
import com.zaneschepke.wireguardautotunnel.cli.commands.tunnel.TunnelCommand
import com.zaneschepke.wireguardautotunnel.cli.provider.ManifestVersionProvider
import picocli.CommandLine.Command

@Command(
    name = "wgtunnel",
    description = ["CLI client for WG Tunnel."],
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider::class,
    header = [BANNER],
    subcommands = [
        TunnelCommand::class
    ]
)
class CliRoot : Runnable {
    override fun run() {

    }
    companion object {
        const val BANNER: String = (""
                + "██╗    ██╗ ██████╗     ████████╗██╗   ██╗███╗   ██╗███╗   ██╗███████╗██╗     \n"
                + "██║    ██║██╔════╝     ╚══██╔══╝██║   ██║████╗  ██║████╗  ██║██╔════╝██║     \n"
                + "██║ █╗ ██║██║  ███╗       ██║   ██║   ██║██╔██╗ ██║██╔██╗ ██║█████╗  ██║     \n"
                + "██║███╗██║██║   ██║       ██║   ██║   ██║██║╚██╗██║██║╚██╗██║██╔══╝  ██║     \n"
                + "╚███╔███╔╝╚██████╔╝       ██║   ╚██████╔╝██║ ╚████║██║ ╚████║███████╗███████╗\n"
                + " ╚══╝╚══╝  ╚═════╝        ╚═╝    ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝╚══════╝\n")

    }
}
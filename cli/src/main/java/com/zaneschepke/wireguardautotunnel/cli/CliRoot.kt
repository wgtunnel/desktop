package com.zaneschepke.wireguardautotunnel.cli

import com.zaneschepke.wireguardautotunnel.cli.CliRoot.Companion.BANNER
import com.zaneschepke.wireguardautotunnel.cli.commands.killswitch.KillSwitchCommand
import com.zaneschepke.wireguardautotunnel.cli.commands.status.StatusCommand
import com.zaneschepke.wireguardautotunnel.cli.commands.tunnel.TunnelCommand
import com.zaneschepke.wireguardautotunnel.cli.provider.ManifestVersionProvider
import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
    name = "wgtctl",
    description = ["A CLI client for WG Tunnel."],
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider::class,
    header = ["@|bold,cyan $BANNER |@"],
    descriptionHeading = "@|bold,underline Description:|@%n",
    parameterListHeading = "@|bold,underline Parameters:|@%n",
    optionListHeading = "@|bold,underline Options:|@%n",
    commandListHeading = "@|bold,underline Commands:|@%n",
    usageHelpAutoWidth = true,
    subcommands = [TunnelCommand::class, KillSwitchCommand::class, StatusCommand::class]
)
class CliRoot : Runnable {
    override fun run() {
        // always show help for no command
        CommandLine(this).usage(System.out)
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
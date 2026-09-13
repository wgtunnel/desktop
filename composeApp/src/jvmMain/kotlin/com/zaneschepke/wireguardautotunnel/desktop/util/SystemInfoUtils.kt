package com.zaneschepke.wireguardautotunnel.desktop.util

import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.systeminfo.SystemInfo

fun buildSystemInfoReport(): String {
    val lines = mutableListOf<String>()

    SystemInfo.osInfo()?.let { os ->
        val distro =
            if (Platform.Current == Platform.Linux) {
                os.distributionId?.takeIf { it.isNotBlank() }
            } else {
                os.longOsVersion?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(os.name, os.osVersion).joinToString(" ").takeIf {
                        it.isNotBlank()
                    }
            }
        distro?.let { lines += "Distro: $it" }
        os.kernelVersion?.takeIf { it.isNotBlank() }?.let { lines += "Kernel: $it" }
        os.cpuArch?.takeIf { it.isNotBlank() }?.let { lines += "Architecture: $it" }
    }

    if (Platform.Current == Platform.Linux) {
        val displayServer =
            when {
                Platform.isWayland -> "Wayland"
                !System.getenv("DISPLAY").isNullOrBlank() -> "X11"
                else -> "Unknown"
            }
        lines += "Display server: $displayServer"
        detectLinuxDesktopEnvironment()?.let { lines += "Desktop: $it" }
    }

    return lines.joinToString("\n")
}

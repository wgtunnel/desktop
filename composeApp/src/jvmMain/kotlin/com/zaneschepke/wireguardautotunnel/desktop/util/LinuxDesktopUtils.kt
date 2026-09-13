package com.zaneschepke.wireguardautotunnel.desktop.util

// Best-effort desktop environment / window manager name via the standard XDG session env vars
fun detectLinuxDesktopEnvironment(): String? =
    System.getenv("XDG_CURRENT_DESKTOP")
        ?.split(":")
        ?.firstOrNull { it.isNotBlank() }
        ?: System.getenv("XDG_SESSION_DESKTOP")?.takeIf { it.isNotBlank() }
        ?: System.getenv("DESKTOP_SESSION")?.takeIf { it.isNotBlank() }

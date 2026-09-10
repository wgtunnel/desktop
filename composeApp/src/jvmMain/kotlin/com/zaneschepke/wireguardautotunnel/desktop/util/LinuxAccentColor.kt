package com.zaneschepke.wireguardautotunnel.desktop.util

import androidx.compose.ui.graphics.Color
import java.util.concurrent.TimeUnit

// GNOME/libadwaita's accent-color palette (introduced in GNOME 47)
private val GNOME_ACCENT_COLORS =
    mapOf(
        "blue" to Color(0xFF3584E4),
        "teal" to Color(0xFF2190A4),
        "green" to Color(0xFF3A944A),
        "yellow" to Color(0xFFC88800),
        "orange" to Color(0xFFED5B00),
        "red" to Color(0xFFE62D42),
        "pink" to Color(0xFFD56199),
        "purple" to Color(0xFF9141AC),
        "slate" to Color(0xFF6F8396),
    )

internal fun runCommand(vararg command: String): String? =
    runCatching {
            val process = ProcessBuilder(*command).redirectErrorStream(true).start()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            if (process.exitValue() != 0) return@runCatching null
            process.inputStream.bufferedReader().readText().trim()
        }
        .getOrNull()

private fun commandExists(command: String): Boolean = runCommand("which", command) != null

private fun parseHexColor(hex: String): Color? {
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6 || cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
    return runCatching { Color(0xFF000000L or cleaned.toLong(16)) }.getOrNull()
}

fun linuxAccentColor(): Color? =
    if (commandExists("omarchy-theme-color")) {
        runCommand("omarchy-theme-color", "accent")?.let(::parseHexColor)
    } else {
        val output = runCommand("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
        GNOME_ACCENT_COLORS[output?.trim('\'', '"')?.lowercase()]
    }

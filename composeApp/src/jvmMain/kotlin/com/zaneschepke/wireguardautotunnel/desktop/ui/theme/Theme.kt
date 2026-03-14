package com.zaneschepke.wireguardautotunnel.desktop.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.kdroid.composetray.lib.mac.MacOSMenuBarThemeDetector.isDark
import com.materialkolor.rememberDynamicColorScheme
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import io.github.kdroidfilter.nucleus.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.nucleus.systemcolor.systemAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WGTunnelTheme(theme: Theme, useSystemColors: Boolean = false, content: @Composable () -> Unit) {
    var isAmoled = false
    val isDark =
        when (theme) {
            Theme.LIGHT -> false
            Theme.DARK -> true
            Theme.AMOLED -> {
                isAmoled = true
                true
            }
            Theme.SYSTEM -> isSystemInDarkMode()
        }
    val seedColor = if (useSystemColors) systemAccentColor() ?: ElectricTeal else ElectricTeal
    val colorScheme =
        rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark, isAmoled = isAmoled)
    MaterialTheme(colorScheme = colorScheme, typography = InterTypography(), content = content)
}

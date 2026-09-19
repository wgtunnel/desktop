package com.zaneschepke.wireguardautotunnel.desktop.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.desktop.util.linuxAccentColor
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.systemcolor.systemAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WGTunnelTheme(
    theme: Theme,
    useSystemColors: Boolean = false,
    customSeedColor: Color? = null,
    accentStyle: AccentStyle = AccentStyle.TONAL_SPOT,
    content: @Composable () -> Unit,
) {
    var isAmoled = false
    val isDark =
        when (theme) {
            Theme.DEFAULT -> true
            Theme.LIGHT -> false
            Theme.DARK -> true
            Theme.AMOLED -> {
                isAmoled = true
                true
            }
            Theme.SYSTEM -> isSystemInDarkMode()
        }

    val seedColor =
        when {
            customSeedColor != null -> customSeedColor
            useSystemColors ->
                systemAccentColor()
                    ?: (if (Platform.Current == Platform.Linux) linuxAccentColor() else null)
                    ?: ElectricTeal
            theme == Theme.DEFAULT -> {
                Aqua
            }
            else -> ElectricTeal
        }
    val colorScheme =
        rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                isAmoled = isAmoled,
                style = accentStyle.toPaletteStyle(),
            )
            .let { scheme ->
                if (theme == Theme.DEFAULT) {
                    scheme.copy(background = Shark, surface = Shark)
                } else {
                    scheme
                }
            }
    // Animates each color individually so a theme change transitions smoothly
    // without tearing down and rebuilding the composition below
    MaterialTheme(
        colorScheme = animateColorScheme(colorScheme),
        typography = InterTypography(),
        content = content,
    )
}

private fun AccentStyle.toPaletteStyle(): PaletteStyle =
    when (this) {
        AccentStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        AccentStyle.NEUTRAL -> PaletteStyle.Neutral
        AccentStyle.VIBRANT -> PaletteStyle.Vibrant
        AccentStyle.EXPRESSIVE -> PaletteStyle.Expressive
        AccentStyle.RAINBOW -> PaletteStyle.Rainbow
        AccentStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        AccentStyle.MONOCHROME -> PaletteStyle.Monochrome
        AccentStyle.FIDELITY -> PaletteStyle.Fidelity
        AccentStyle.CONTENT -> PaletteStyle.Content
    }

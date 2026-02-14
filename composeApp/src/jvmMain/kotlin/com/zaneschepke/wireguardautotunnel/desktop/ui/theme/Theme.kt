package com.zaneschepke.wireguardautotunnel.desktop.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WGTunnelTheme(theme: Theme, seedColor: Color = ElectricTeal, content: @Composable () -> Unit) {
    var isAmoled = false
    val isDark =
        when (theme) {
            Theme.LIGHT -> false
            Theme.DARK -> true
            Theme.AMOLED -> {
                isAmoled = true
                true
            }
        }
    val colorScheme =
        rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark, isAmoled = isAmoled)
    MaterialTheme(colorScheme = colorScheme, typography = InterTypography(), content = content)
}

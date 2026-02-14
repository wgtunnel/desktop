package com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.NavController

@Composable
fun rememberNavController(
    backStack: NavBackStack<NavKey>,
    onChange: (NavKey?) -> Unit = {},
    onExitApp: () -> Unit = {},
): NavController {
    return remember(backStack, onChange, onExitApp) {
        NavController(backStack, onChange, onExitApp)
    }
}

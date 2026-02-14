package com.zaneschepke.wireguardautotunnel.desktop.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavController(
    private val backStack: NavBackStack<NavKey>,
    private val onChange: (previous: NavKey?) -> Unit = {},
    private val onExitApp: () -> Unit = {},
) {
    fun push(route: NavKey) {
        onChange(currentRoute)
        backStack.add(route)
    }

    fun pop(): Boolean {
        if (!canPop) {
            onExitApp()
            return true
        }
        onChange(currentRoute)
        backStack.removeLastOrNull()
        return true
    }

    fun popUpTo(route: NavKey) {
        onChange(currentRoute)
        backStack.clear()
        if (route is Route.Tunnels) backStack.add(route)
        else backStack.addAll(setOf(Route.Tunnels, route))
    }

    val currentRoute: NavKey?
        get() = backStack.lastOrNull()

    val canPop: Boolean
        get() = backStack.size > 1
}

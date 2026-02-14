package com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.routeSerializersModule

@Composable
fun rememberNavBackStack(startingStack: List<NavKey>): NavBackStack<NavKey> {
    val config = SavedStateConfiguration { serializersModule = routeSerializersModule() }
    return rememberNavBackStack(config, *startingStack.toTypedArray())
}

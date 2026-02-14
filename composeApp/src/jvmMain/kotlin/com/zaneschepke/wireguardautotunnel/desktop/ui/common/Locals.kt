package com.zaneschepke.wireguardautotunnel.desktop.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.dokar.sonner.ToasterState
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.NavController

val LocalNavController =
    staticCompositionLocalOf<NavController> { error("No navigation controller provided") }

val LocalToaster = staticCompositionLocalOf<ToasterState> { error("No ToasterState provided") }

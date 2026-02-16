package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dokar.sonner.ToasterState
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.animation.NoRippleInteractionSource
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.animation.PulsingStatusLed
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Tab
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions.rememberNavBackStack
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions.rememberNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate.DonateScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate.crypto.AddressesScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.license.LicenseScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.TunnelsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.TunnelScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import kotlin.collections.listOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun App(uiState: AppUiState, viewModel: AppViewModel, toaster: ToasterState) {
    val backStack = rememberNavBackStack(listOf<NavKey>(Route.Tunnels))
    val currentRoute by remember { derivedStateOf { backStack.lastOrNull() as? Route } }
    var previousRoute by remember { mutableStateOf<Route?>(null) }
    val currentTab by remember { derivedStateOf { Tab.fromRoute(currentRoute ?: Route.Tunnels) } }
    val navController =
        rememberNavController(
            backStack,
            onExitApp = {},
            onChange = { previousKey -> previousRoute = previousKey as? Route },
        )

    val railState = rememberWideNavigationRailState(WideNavigationRailValue.Collapsed)
    val scope = rememberCoroutineScope()

    val headerDescription =
        if (railState.targetValue == WideNavigationRailValue.Expanded) {
            "Collapse rail"
        } else {
            "Expand rail"
        }

    if (!uiState.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LocalinaApp {
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalToaster provides toaster,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                WideNavigationRail(
                    state = railState,
                    arrangement = Arrangement.SpaceBetween,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            CustomTooltip(text = headerDescription) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (
                                                railState.targetValue ==
                                                    WideNavigationRailValue.Expanded
                                            )
                                                railState.collapse()
                                            else railState.expand()
                                        }
                                    },
                                    modifier =
                                        Modifier.padding(start = 24.dp).semantics {
                                            stateDescription =
                                                if (
                                                    railState.currentValue ==
                                                        WideNavigationRailValue.Expanded
                                                )
                                                    "Expanded"
                                                else "Collapsed"
                                        },
                                ) {
                                    if (railState.targetValue == WideNavigationRailValue.Expanded) {
                                        Icon(Icons.AutoMirrored.Filled.MenuOpen, headerDescription)
                                    } else {
                                        Icon(Icons.Filled.Menu, headerDescription)
                                    }
                                }
                            }
                        }
                    },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Tab.entries.forEach { tab ->
                            WideNavigationRailItem(
                                railExpanded =
                                    railState.targetValue == WideNavigationRailValue.Expanded,
                                selected = currentTab == tab,
                                onClick = { navController.popUpTo(tab.startRoute) },
                                icon = { Icon(tab.activeIcon, null) },
                                label = { Text(stringResource(tab.titleRes)) },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (uiState.lockdownActive) {
                            WideNavigationRailItem(
                                interactionSource = remember { NoRippleInteractionSource() },
                                selected = false,
                                railExpanded =
                                    railState.targetValue == WideNavigationRailValue.Expanded,
                                icon = {
                                    CustomTooltip(text = "Lockdown active") {
                                        Icon(Icons.Filled.Lock, "Lockdown active", tint = AlertRed)
                                    }
                                },
                                enabled = false,
                                label = {},
                                onClick = {},
                            )
                        }
                        WideNavigationRailItem(
                            interactionSource = remember { NoRippleInteractionSource() },
                            selected = false,
                            railExpanded =
                                railState.targetValue == WideNavigationRailValue.Expanded,
                            icon = {
                                CustomTooltip(text = "Daemon health") {
                                    PulsingStatusLed(isHealthy = uiState.daemonConnected)
                                }
                            },
                            enabled = false,
                            label = {},
                            onClick = {},
                        )
                    }
                }
                Scaffold {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { navController.pop() },
                        transitionSpec = {
                            val initialIndex = previousRoute?.let(Tab::fromRoute)?.index ?: 0
                            val targetIndex = currentRoute?.let(Tab::fromRoute)?.index ?: 0

                            if (initialIndex != targetIndex) {
                                val isMovingDown = targetIndex > initialIndex
                                (fadeIn(tween(200)) +
                                    slideInVertically(tween(200)) {
                                        if (isMovingDown) 30 else -30
                                    }) togetherWith (fadeOut(tween(150)))
                            } else {
                                (fadeIn(tween(200)) +
                                    scaleIn(
                                        initialScale = 0.95f,
                                        animationSpec = tween(200),
                                    )) togetherWith fadeOut(tween(150))
                            }
                        },
                        popTransitionSpec = {
                            (fadeIn(tween(200)) +
                                scaleIn(
                                    initialScale = 1.05f,
                                    animationSpec = tween(200),
                                )) togetherWith
                                (fadeOut(tween(150)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                        },
                        predictivePopTransitionSpec = {
                            (fadeIn(tween(200)) +
                                scaleIn(
                                    initialScale = 1.05f,
                                    animationSpec = tween(200),
                                )) togetherWith
                                (fadeOut(tween(150)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                        },
                        entryDecorators =
                            listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                        entryProvider =
                            entryProvider {
                                currentTab.startRoute
                                entry<Route.Tunnels> { TunnelsScreen() }
                                entry<Route.Tunnel> {
                                    val viewModel: TunnelViewModel =
                                        koinViewModel(parameters = { parametersOf(it.id) })
                                    TunnelScreen(viewModel)
                                }
                                entry<Route.Settings> { SettingsScreen() }
                                entry<Route.AutoTunnel> { AutoTunnelScreen() }
                                entry<Route.Support> { SupportScreen() }
                                entry<Route.License> { LicenseScreen() }
                                entry<Route.Donate> { DonateScreen(viewModel) }
                                entry<Route.Addresses> { AddressesScreen() }
                                entry<Route.Appearance> { AppearanceScreen() }
                                entry<Route.Display> { DisplayScreen(viewModel) }
                            },
                    )
                }
            }
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.auto_tunnel_active
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.check_for_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.connecting_to_daemon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.daemon_connected
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.syncing_with_daemon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.lockdown_active
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.animation.PulsingStatusLed
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.CommandToastMessage
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.CopyCommandAction
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast.NavigateAction
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.tooltip.CustomTooltip
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Tab
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions.rememberNavBackStack
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.functions.rememberNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.preferred.PreferredTunnelScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.wifi.WifiSettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.dns.DnsSettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.globals.GlobalConfigScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.globals.TunnelGlobalsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.lockdown.LockdownSettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.monitoring.MonitoringScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.proxy.ProxySettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.recovery.TunnelRecoveryScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate.DonateScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate.crypto.AddressesScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.license.LicenseScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.TunnelsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.ConfigScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.TunnelSettingsScreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.DaemonConnectionStatus
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.ErrorRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.HealthyGreen
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WarningAmber
import com.zaneschepke.wireguardautotunnel.desktop.util.toClipEntry
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.TunnelViewModel
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import kotlin.collections.listOf
import kotlin.time.Duration
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectSideEffect

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
    val clipboard = LocalClipboard.current
    val checkForUpdateLabel = stringResource(Res.string.check_for_update)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            is AppSideEffect.ActionableToast ->
                toaster.show(
                    Toast(
                        message =
                            CommandToastMessage(
                                description = sideEffect.message,
                                command = sideEffect.copyText,
                            ),
                        id = sideEffect.id,
                        action =
                            CopyCommandAction(sideEffect.copyLabel) {
                                scope.launch {
                                    clipboard.setClipEntry(sideEffect.copyText.toClipEntry())
                                }
                            },
                        type = sideEffect.type,
                        duration = Duration.INFINITE,
                    )
                )
            is AppSideEffect.UpdateAvailableToast ->
                toaster.show(
                    Toast(
                        message = sideEffect.message,
                        id = sideEffect.id,
                        action =
                            NavigateAction(checkForUpdateLabel) {
                                navController.push(Route.Support)
                            },
                        type = ToastType.Info,
                        duration = Duration.INFINITE,
                    )
                )
            is AppSideEffect.DismissToast -> toaster.dismiss(sideEffect.id)
        }
    }

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
            Crossfade(
                targetState = uiState.theme to uiState.useSystemColors,
                animationSpec = tween(250),
                label = "ThemeChange",
            ) { (theme, useSystemColors) ->
                Column(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        WideNavigationRail(
                            state = railState,
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
                                            if (
                                                railState.targetValue ==
                                                    WideNavigationRailValue.Expanded
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.MenuOpen,
                                                    headerDescription,
                                                )
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
                                            railState.targetValue ==
                                                WideNavigationRailValue.Expanded,
                                        selected = currentTab == tab,
                                        onClick = { navController.popUpTo(tab.startRoute) },
                                        icon = { Icon(tab.activeIcon, null) },
                                        label = { Text(stringResource(tab.titleRes)) },
                                    )
                                }
                            }
                        }
                        Scaffold(containerColor = Color.Transparent) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { navController.pop() },
                                transitionSpec = {
                                    val initialIndex =
                                        previousRoute?.let(Tab::fromRoute)?.index ?: 0
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
                                            scaleOut(
                                                targetScale = 0.95f,
                                                animationSpec = tween(150),
                                            ))
                                },
                                predictivePopTransitionSpec = {
                                    (fadeIn(tween(200)) +
                                        scaleIn(
                                            initialScale = 1.05f,
                                            animationSpec = tween(200),
                                        )) togetherWith
                                        (fadeOut(tween(150)) +
                                            scaleOut(
                                                targetScale = 0.95f,
                                                animationSpec = tween(150),
                                            ))
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
                                            TunnelSettingsScreen(viewModel)
                                        }
                                        entry<Route.Config> {
                                            val viewModel: TunnelViewModel =
                                                koinViewModel(parameters = { parametersOf(it.id) })
                                            ConfigScreen(viewModel, live = false)
                                        }
                                        entry<Route.LiveConfig> {
                                            val viewModel: TunnelViewModel =
                                                koinViewModel(parameters = { parametersOf(it.id) })
                                            ConfigScreen(viewModel, live = true)
                                        }
                                        entry<Route.Settings> { SettingsScreen() }
                                        entry<Route.Logs> { LogsScreen() }
                                        entry<Route.Dns> { DnsSettingsScreen() }
                                        entry<Route.TunnelGlobals> { TunnelGlobalsScreen() }
                                        entry<Route.ConfigGlobal> { GlobalConfigScreen() }
                                        entry<Route.ProxySettings> { ProxySettingsScreen() }
                                        entry<Route.LockdownSettings> { LockdownSettingsScreen() }
                                        entry<Route.TunnelRecovery> { TunnelRecoveryScreen() }
                                        entry<Route.TunnelMonitoring> { MonitoringScreen() }
                                        entry<Route.AutoTunnel> { AutoTunnelScreen() }
                                        entry<Route.WifiPreferences> { WifiSettingsScreen() }
                                        entry<Route.PreferredTunnel> {
                                            PreferredTunnelScreen(it.tunnelNetwork)
                                        }
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
                    StatusFooter(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun StatusFooter(uiState: AppUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiState.lockdownActive) {
            val lockdownActiveText = stringResource(Res.string.lockdown_active)
            CustomTooltip(text = lockdownActiveText) {
                Icon(
                    Icons.Filled.Lock,
                    lockdownActiveText,
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (uiState.autoTunnelEnabled) {
            val autoTunnelActiveText = stringResource(Res.string.auto_tunnel_active)
            CustomTooltip(text = autoTunnelActiveText) {
                Icon(
                    Icons.Filled.Bolt,
                    autoTunnelActiveText,
                    tint = HealthyGreen,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        CustomTooltip(
            text =
                stringResource(
                    when (uiState.daemonStatus) {
                        DaemonConnectionStatus.CONNECTED -> Res.string.daemon_connected
                        DaemonConnectionStatus.SYNCING -> Res.string.syncing_with_daemon
                        DaemonConnectionStatus.DISCONNECTED -> Res.string.connecting_to_daemon
                    }
                )
        ) {
            PulsingStatusLed(
                color =
                    when (uiState.daemonStatus) {
                        DaemonConnectionStatus.CONNECTED -> HealthyGreen
                        DaemonConnectionStatus.SYNCING -> WarningAmber
                        DaemonConnectionStatus.DISCONNECTED -> ErrorRed
                    }
            )
        }
    }
}

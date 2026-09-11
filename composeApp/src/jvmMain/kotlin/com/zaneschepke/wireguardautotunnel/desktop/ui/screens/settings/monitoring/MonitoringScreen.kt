package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.monitoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.enums.StatisticRefresh
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.live_tunnel_statistics
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.refresh_rate
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.statistics
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_monitoring
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dropdown.LabeledDropdown
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.MonitoringViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(viewModel: MonitoringViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    if (!uiState.isLoaded) return

    NestedSettingsScaffold(title = stringResource(Res.string.tunnel_monitoring)) { padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.statistics),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
                    title = stringResource(Res.string.live_tunnel_statistics),
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.tunnelStatisticsEnabled,
                            onClick = viewModel::onLiveTunnelStatisticsChanged,
                        )
                    },
                    onClick = {
                        viewModel.onLiveTunnelStatisticsChanged(!uiState.tunnelStatisticsEnabled)
                    },
                )
                LabeledDropdown(
                    title = stringResource(Res.string.refresh_rate),
                    leading = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                    currentValue = uiState.statisticRefresh,
                    onSelected = { selected ->
                        selected?.let { viewModel.onStatisticsIntervalChanged(it) }
                    },
                    options = StatisticRefresh.entries,
                    optionToString = { (it ?: StatisticRefresh.BALANCED).label },
                )
            }
        }
    }
}

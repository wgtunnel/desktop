package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SeamlessRecoveryBounceDelay
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.recovery_bounce_delay
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.recovery_bounce_delay_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.seamless_recovery
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.seamless_recovery_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tunnel_recovery
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dropdown.LabeledDropdown
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.util.asLabel
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TunnelRecoveryScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
        }
    }

    if (!uiState.isLoaded) return

    val bounceDelay =
        SeamlessRecoveryBounceDelay.fromSeconds(uiState.settings.seamlessRecoveryBounceDelaySec)

    NestedSettingsScaffold(title = stringResource(Res.string.tunnel_recovery)) { padding ->
        ScrollableColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.tunnel_recovery),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                    title = stringResource(Res.string.seamless_recovery),
                    trailing = { modifier ->
                        ThemedSwitch(
                            checked = uiState.settings.seamlessRecoveryEnabled,
                            onClick = { viewModel.onSeamlessRecovery(it) },
                            modifier = modifier,
                        )
                    },
                    description = {
                        DescriptionText(stringResource(Res.string.seamless_recovery_desc))
                    },
                    onClick = {
                        viewModel.onSeamlessRecovery(!uiState.settings.seamlessRecoveryEnabled)
                    },
                )
                LabeledDropdown(
                    title = stringResource(Res.string.recovery_bounce_delay),
                    description = {
                        DescriptionText(stringResource(Res.string.recovery_bounce_delay_desc))
                    },
                    leading = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                    currentValue = bounceDelay,
                    onSelected = { selected ->
                        selected?.let { viewModel.onSeamlessRecoveryBounceDelay(it.seconds) }
                    },
                    options = SeamlessRecoveryBounceDelay.entries,
                    optionToString = { (it ?: SeamlessRecoveryBounceDelay.THIRTY).asLabel() },
                )
            }
        }
    }
}

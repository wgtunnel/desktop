package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.already_donated
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.already_donated_description
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.back
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.crypto
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donate
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.github
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.github_sponsors
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.github_sponsors_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kofi
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.kofi_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.liberapay
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.liberapay_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.options
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components.DonationHeroSection
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(appViewModel: AppViewModel) {

    val uiState by appViewModel.collectAsState()

    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.donate)) },
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(it),
        ) {
            DonationHeroSection()
            Column {
                GroupLabel(stringResource(Res.string.options), Modifier.padding(horizontal = 16.dp))
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.CurrencyBitcoin, contentDescription = null) },
                    title = stringResource(Res.string.crypto),
                    onClick = { navController.push(Route.Addresses) },
                )
                val githubSponsorsUrl = stringResource(Res.string.github_sponsors_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.github),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.github_sponsors),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(githubSponsorsUrl) },
                )
                val liberapayUrl = stringResource(Res.string.liberapay_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.liberapay),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.liberapay),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(liberapayUrl) },
                )
                val kofiUrl = stringResource(Res.string.kofi_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.kofi),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.kofi),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(kofiUrl) },
                )
                SurfaceRow(
                    leading = {
                        Icon(
                            Icons.Outlined.Done,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.already_donated),
                    description = {
                        DescriptionText(stringResource(Res.string.already_donated_description))
                    },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.alreadyDonated,
                            onClick = { checked -> appViewModel.setAlreadyDonated(checked) },
                        )
                    },
                    onClick = { appViewModel.setAlreadyDonated(!uiState.alreadyDonated) },
                )
            }
        }
    }
}

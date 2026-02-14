package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.*
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(appViewModel: AppViewModel) {

    val navController = LocalNavController.current

    val uiState by appViewModel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.display_theme)) },
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
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            enumValues<Theme>().forEach {
                val title =
                    when (it) {
                        Theme.DARK -> stringResource(Res.string.dark)
                        Theme.LIGHT -> stringResource(Res.string.light)
                        Theme.AMOLED -> stringResource(Res.string.amoled)
                    }
                SurfaceRow(
                    title = title,
                    trailing =
                        if (uiState.theme == it) {
                            {
                                Icon(
                                    Icons.Outlined.Check,
                                    stringResource(Res.string.selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    onClick = { appViewModel.setTheme(it) },
                )
            }
        }
    }
}

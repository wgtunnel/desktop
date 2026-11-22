package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.automatic
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.back
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.language
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.selected
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import java.text.Collator
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(appViewModel: AppViewModel) {
    val navController = LocalNavController.current
    val uiState by appViewModel.collectAsState()

    val collator = remember { Collator.getInstance(Locale.getDefault()) }
    val sortedLocales = remember {
        LocaleUtil.supportedLocales
            .map { Locale.forLanguageTag(it) }
            .sortedWith(compareBy(collator) { it.getDisplayName(it) })
    }

    val lazyListState = rememberLazyListState()

    val selectedIndex =
        remember(uiState.locale, sortedLocales) {
            if (uiState.locale == null) 0
            else sortedLocales.indexOfFirst { it.toLanguageTag() == uiState.locale } + 1
        }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) lazyListState.scrollToItem(selectedIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.language)) },
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SurfaceRow(
                    title = stringResource(Res.string.automatic),
                    selected = uiState.locale == null,
                    trailing =
                        if (uiState.locale == null) {
                            { _ ->
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(Res.string.selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    onClick = { appViewModel.setLocale(null) },
                )
            }
            items(sortedLocales, key = { it.toLanguageTag() }) { locale ->
                val tag = locale.toLanguageTag()
                val isSelected = uiState.locale == tag
                SurfaceRow(
                    title =
                        locale.getDisplayName(locale).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                        },
                    selected = isSelected,
                    trailing =
                        if (isSelected) {
                            { _ ->
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(Res.string.selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    onClick = { appViewModel.setLocale(tag) },
                )
            }
        }
    }
}

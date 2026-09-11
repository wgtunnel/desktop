package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.logs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dokar.sonner.Toast
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.delete_logs
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.export_logs
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.logs
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.no_logs_yet
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold.NestedSettingsScaffold
import com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.logs.components.LogList
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.LoggerViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LogsScreen(viewModel: LoggerViewModel = koinViewModel()) {
    val toaster = LocalToaster.current
    val loggerState by viewModel.collectAsState()
    val lazyColumnListState = rememberLazyListState()
    var isAutoScrolling by rememberSaveable { mutableStateOf(true) }
    var lastScrollPosition by rememberSaveable { mutableIntStateOf(0) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AppSideEffect.Toast -> toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    if (loggerState.isLoading) return

    LaunchedEffect(isAutoScrolling, loggerState.messages.size) {
        if (isAutoScrolling && loggerState.messages.isNotEmpty()) {
            lazyColumnListState.animateScrollToItem(loggerState.messages.lastIndex)
        }
    }

    LaunchedEffect(lazyColumnListState) {
        snapshotFlow { lazyColumnListState.firstVisibleItemIndex }
            .collect { currentScrollPosition ->
                if (currentScrollPosition < lastScrollPosition && isAutoScrolling) {
                    isAutoScrolling = false
                }
                val visible = lazyColumnListState.layoutInfo.visibleItemsInfo
                if (
                    visible.isNotEmpty() &&
                        visible.last().index ==
                            lazyColumnListState.layoutInfo.totalItemsCount - 1 &&
                        !isAutoScrolling
                ) {
                    isAutoScrolling = true
                }
                lastScrollPosition = currentScrollPosition
            }
    }

    val exportLabel = stringResource(Res.string.export_logs)
    val deleteLabel = stringResource(Res.string.delete_logs)

    NestedSettingsScaffold(
        title = stringResource(Res.string.logs),
        actions = {
            IconButton(onClick = { viewModel.exportLogs() }) {
                Icon(Icons.Outlined.FolderZip, contentDescription = exportLabel)
            }
            IconButton(onClick = { viewModel.deleteLogs() }) {
                Icon(Icons.Outlined.Delete, contentDescription = deleteLabel)
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = loggerState.messages.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "LogsContentTransition",
            modifier = Modifier.padding(padding),
        ) { isEmpty ->
            if (isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.no_logs_yet))
                }
            } else {
                LogList(
                    logs = loggerState.messages,
                    lazyColumnListState = lazyColumnListState,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.logs.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollbarThickness
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.appScrollbarStyle

@Composable
fun LogList(
    logs: List<LogMessageDto>,
    lazyColumnListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = lazyColumnListState,
            modifier =
                Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(end = ScrollbarThickness),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items = logs, key = { index, _ -> index }) { _, log -> LogItem(log = log) }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyColumnListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = appScrollbarStyle(),
        )
    }
}

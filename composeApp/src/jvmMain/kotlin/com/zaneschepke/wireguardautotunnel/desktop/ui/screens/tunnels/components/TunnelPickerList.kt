package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.search
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollbarThickness
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.appScrollbarStyle
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox.ConfigurationTextBox
import org.jetbrains.compose.resources.stringResource

@Composable
fun TunnelPickerList(
    tunnels: List<TunnelConfig>,
    isSelected: (TunnelConfig) -> Boolean,
    onSelect: (TunnelConfig) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    leadingItem: (@Composable () -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered =
        remember(tunnels, query) { tunnels.filter { it.name.contains(query, ignoreCase = true) } }

    Column(modifier = modifier.fillMaxSize()) {
        ConfigurationTextBox(
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            label = stringResource(Res.string.search),
            hint = stringResource(Res.string.search),
            trailing = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        val lazyListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(end = ScrollbarThickness),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (leadingItem != null) item { leadingItem() }
                items(filtered, key = { it.id }) { tunnel ->
                    val selected = isSelected(tunnel)
                    SurfaceRow(
                        leading = leading,
                        title = tunnel.name,
                        selected = selected,
                        trailing = {
                            if (selected) Icon(Icons.Outlined.Check, contentDescription = null)
                        },
                        onClick = { onSelect(tunnel) },
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                style = appScrollbarStyle(),
            )
        }
    }
}

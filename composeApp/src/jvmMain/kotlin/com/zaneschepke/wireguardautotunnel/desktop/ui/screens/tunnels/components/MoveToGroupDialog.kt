package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.add_group_ellipsis
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.cancel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.move_to_group
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoveToGroupDialog(
    groups: List<TunnelGroup>,
    onDismiss: () -> Unit,
    onSelect: (TunnelGroup) -> Unit,
    onNewGroup: () -> Unit,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(Res.string.move_to_group)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        SurfaceRow(
                            title = stringResource(Res.string.add_group_ellipsis),
                            leading = {
                                Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                            },
                            onClick = onNewGroup,
                        )
                        if (groups.isNotEmpty()) HorizontalDivider()
                        groups
                            .sortedBy { it.name.lowercase() }
                            .forEach { group ->
                                SurfaceRow(
                                    title = group.name,
                                    leading = {
                                        Icon(Icons.Outlined.Folder, contentDescription = null)
                                    },
                                    onClick = { onSelect(group) },
                                )
                            }
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = true),
            )
        }
    }
}

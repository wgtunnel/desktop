package com.zaneschepke.wireguardautotunnel.desktop.ui.common.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.selected
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import org.jetbrains.compose.resources.stringResource

data class PickerOption(
    val leadingIcon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val selected: Boolean = false,
    val description: String? = null,
)

@Composable
fun OptionPickerMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<PickerOption>,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 320.dp, max = 420.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        options.forEachIndexed { index, option ->
            SurfaceRow(
                title = option.label,
                onClick = option.onClick,
                leading = { Icon(imageVector = option.leadingIcon, contentDescription = null) },
                trailing =
                    if (option.selected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(Res.string.selected),
                            )
                        }
                    } else null,
                description = option.description?.let { { DescriptionText(it) } },
            )
            if (index != options.lastIndex) HorizontalDivider()
        }
    }
}

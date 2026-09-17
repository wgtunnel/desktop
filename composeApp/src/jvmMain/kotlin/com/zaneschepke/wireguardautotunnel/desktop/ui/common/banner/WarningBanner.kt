package com.zaneschepke.wireguardautotunnel.desktop.ui.common.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.warning
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.WarningAmber
import org.jetbrains.compose.resources.stringResource

@Composable
fun WarningBanner(
    title: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable (Modifier) -> Unit)? = null,
) {
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically()) {
        SurfaceRow(
            title = title,
            modifier = modifier,
            leading = {
                Icon(Icons.Outlined.Warning, stringResource(Res.string.warning), tint = WarningAmber)
            },
            trailing = trailing,
            onClick = onClick,
        )
    }
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.common.button

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.Disabled

@Composable
fun SurfaceRow(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable ((Modifier) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var leadingPadding by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
                // Full-row clickable + ripple (this is the important part)
                .combinedClickable(
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                    enabled = enabled && onClick != null,
                    interactionSource = interactionSource,
                    indication = null, // we add the ripple manually below so it covers the whole row
                )
                .indication(interactionSource, ripple())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 48.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (leading != null) {
                Row(
                    modifier =
                        Modifier.onSizeChanged {
                            leadingPadding = with(density) { it.width.toDp() }
                        }
                ) {
                    leading()
                    Spacer(Modifier.width(16.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Disabled,
                )
                if (description != null) description()
            }

            if (trailing != null) {
                trailing(Modifier)
            }
        }

        if (expandedContent != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = leadingPadding, top = 4.dp)) {
                expandedContent()
            }
        }
    }
}

// String overload (unchanged)
@Composable
fun SurfaceRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null,
    onLongClick: () -> Unit = {},
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable ((Modifier) -> Unit)? = null,
) {
    SurfaceRow(
        title = AnnotatedString(title),
        modifier = modifier,
        onClick = onClick,
        description = description,
        expandedContent = expandedContent,
        onLongClick = onLongClick,
        enabled = enabled,
        selected = selected,
        leading = leading,
        trailing = trailing,
    )
}

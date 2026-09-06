package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.logs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.LogTypeLabel
import com.zaneschepke.wireguardautotunnel.desktop.util.toClipEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val timeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Composable
fun LogItem(log: LogMessageDto) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        scope.launch { clipboard.setClipEntry(log.toString().toClipEntry()) }
                    },
                )
                .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = formatLogTime(log.time),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
            )
            LogTypeLabel(color = Color(levelColor(log.level))) {
                Text(text = log.level, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Text(
                text = "${log.source}/${log.tag}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
}

private fun formatLogTime(timeString: String): String {
    return try {
        timeFormatter.format(Instant.parse(timeString))
    } catch (_: Exception) {
        timeString.takeLast(12)
    }
}

private fun levelColor(level: String): Long =
    when (level) {
        "D" -> 0xFF2196F3
        "I" -> 0xFF4CAF50
        "A" -> 0xFF9C27B0
        "W" -> 0xFFFFC107
        "E" -> 0xFFF44336
        else -> 0xFF9E9E9E
    }

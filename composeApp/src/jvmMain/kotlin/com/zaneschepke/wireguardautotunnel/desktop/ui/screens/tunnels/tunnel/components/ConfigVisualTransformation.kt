package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.tunnels.tunnel.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ConfigVisualTransformation : VisualTransformation {
    private val headerRegex = "\\[(Interface|Peer)]".toRegex()
    private val keyRegex = "(?m)^[a-zA-Z0-9]+(?=\\s*=)".toRegex()
    private val commentRegex = "#.*".toRegex()

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val rawText = text.text

        // Headers
        headerRegex.findAll(rawText).forEach {
            builder.addStyle(
                SpanStyle(color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold),
                it.range.first,
                it.range.last + 1,
            )
        }

        // Keys
        keyRegex.findAll(rawText).forEach {
            builder.addStyle(
                SpanStyle(color = Color(0xFF03DAC5)),
                it.range.first,
                it.range.last + 1,
            )
        }

        // Comments
        commentRegex.findAll(rawText).forEach { match ->
            val trimmed = match.value.trimEnd()
            if (trimmed.isNotEmpty()) {
                builder.addStyle(
                    SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.first + trimmed.length,
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

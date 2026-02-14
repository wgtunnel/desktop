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
    override fun filter(text: AnnotatedString): TransformedText {

        val builder = AnnotatedString.Builder(text)

        val rawText = text.text

        // highlight headers
        "\\[(Interface|Peer)]".toRegex().findAll(rawText).forEach {
            builder.addStyle(
                SpanStyle(color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold),
                it.range.first,
                it.range.last + 1,
            )
        }

        // highlight keys
        "(?m)^[a-zA-Z0-9]+(?=\\s*=)".toRegex().findAll(rawText).forEach {
            builder.addStyle(
                SpanStyle(color = Color(0xFF03DAC5)),
                it.range.first,
                it.range.last + 1,
            )
        }

        // highlight comments
        "#.*".toRegex().findAll(rawText).forEach {
            val result = it.value
            val trimmedLength = result.trimEnd().length

            if (trimmedLength > 0) {
                builder.addStyle(
                    SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic),
                    it.range.first,
                    it.range.first + trimmedLength,
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

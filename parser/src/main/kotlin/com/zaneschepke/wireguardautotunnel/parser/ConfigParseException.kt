package com.zaneschepke.wireguardautotunnel.parser

class ConfigParseException(
    val errorType: ErrorType,
    val field: String,
    val value: Any? = null,
    val extra: String? = null,
    message: String =
        "$field: $errorType${value?.let { " (value: $it)" } ?: ""}${extra?.let { " ($it)" } ?: ""}",
) : Exception(message)

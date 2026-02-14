package com.zaneschepke.wireguardautotunnel.parser.util

import com.zaneschepke.wireguardautotunnel.parser.ConfigParseException
import com.zaneschepke.wireguardautotunnel.parser.ErrorType

fun Map<String, String>.getInt(key: String, section: String): Int? {
    val value = this[key] ?: return null
    return value.toIntOrNull()
        ?: throw ConfigParseException(ErrorType.INVALID_VALUE_FORMAT, "$section.$key", value)
}

fun Map<String, String>.getBool(key: String, section: String): Boolean? {
    val value = this[key] ?: return null
    return when (value.lowercase()) {
        "true",
        "yes",
        "on" -> true
        "false",
        "no",
        "off" -> false
        else -> throw ConfigParseException(ErrorType.INVALID_VALUE_FORMAT, "$section.$key", value)
    }
}

fun Map<String, String>.getList(key: String): List<String>? {
    return this[key]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
}

fun Map<String, String>.getLong(key: String, section: String): Long? {
    return this[key]?.toLongOrNull()
        ?: run { throw ConfigParseException(ErrorType.INVALID_VALUE, "$section.$key", this[key]) }
}

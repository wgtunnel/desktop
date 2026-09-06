package com.zaneschepke.wireguardautotunnel.client.data.converter

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

class StringListConverter {
    @ColumnTypeConverter
    fun listToString(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @ColumnTypeConverter
    fun stringToList(value: String): List<String> {
        if (value.isBlank() || value == "[]") return emptyList()
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}

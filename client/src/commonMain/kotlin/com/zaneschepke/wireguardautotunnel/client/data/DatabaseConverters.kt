package com.zaneschepke.wireguardautotunnel.client.data

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class DatabaseConverters {
    @TypeConverter
    fun listToString(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun stringToList(value: String): List<String> {
        if (value.isBlank() || value.isEmpty()) return mutableListOf()
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            val list = value.split(",").toMutableList()
            val json = listToString(list)
            Json.decodeFromString<List<String>>(json)
        }
    }

    @TypeConverter
    fun setToString(value: Set<String>): String {
        return listToString(value.toList())
    }

    @TypeConverter
    fun stringToSet(value: String): Set<String> {
        return stringToList(value).toSet()
    }

    @TypeConverter fun toMode(value: Int): AppMode = AppMode.fromValue(value)

    @TypeConverter fun fromMode(mode: AppMode): Int = mode.value

    @TypeConverter fun toDnsProtocol(value: Int): DnsProtocol = DnsProtocol.fromValue(value)

    @TypeConverter fun fromDnsProtocol(mode: DnsProtocol): Int = mode.value
}

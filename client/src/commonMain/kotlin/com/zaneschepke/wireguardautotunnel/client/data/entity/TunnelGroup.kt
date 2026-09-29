package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "tunnel_group", indices = [Index(value = ["name"], unique = true)])
data class TunnelGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "expanded", defaultValue = "1") val expanded: Boolean = true,
)

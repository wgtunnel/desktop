package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField

@Entity(tableName = "tunnel_config", indices = [Index(value = ["name"], unique = true)])
data class TunnelConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @field:TypeConverters(AppKeyringConverter::class)
    @ColumnInfo(name = "quick_config")
    val quickConfig: EncryptedField,
    @ColumnInfo(name = "active", defaultValue = "false") val active: Boolean = false,
    val isIpv4Preferred: Boolean = true,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
)

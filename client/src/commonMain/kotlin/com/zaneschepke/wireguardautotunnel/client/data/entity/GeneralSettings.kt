package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "general_settings")
data class GeneralSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "theme", defaultValue = "DARK") val theme: String = "DARK",
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "already_donated", defaultValue = "0") val alreadyDonated: Boolean = false,
    @ColumnInfo(name = "restore_tunnel_on_boot", defaultValue = "0")
    val restoreTunnelOnBoot: Boolean = false,
)

package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "lockdown_settings")
data class LockdownSettings(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "lockdown_enabled", defaultValue = "0") val enabled: Boolean = false,
    @ColumnInfo(name = "restore_on_boot", defaultValue = "0") val restoreOnBoot: Boolean = false,
    @ColumnInfo(name = "bypass_lan", defaultValue = "0") val bypassLan: Boolean = false,
)

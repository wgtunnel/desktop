package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode

@Entity(tableName = "general_settings")
data class GeneralSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "is_restore_on_boot_enabled", defaultValue = "0")
    val isRestoreOnBootEnabled: Boolean = false,
    @ColumnInfo(name = "app_mode", defaultValue = "0") val appMode: AppMode = AppMode.fromValue(0),
    @ColumnInfo(name = "theme", defaultValue = "AUTOMATIC") val theme: String = "AUTOMATIC",
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "already_donated", defaultValue = "0") val alreadyDonated: Boolean = false,
)

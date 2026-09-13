package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "general_settings")
data class GeneralSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "theme", defaultValue = "DARK") val theme: String = "DARK",
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "already_donated", defaultValue = "0") val alreadyDonated: Boolean = false,
    @ColumnInfo(name = "restore_tunnel_on_boot", defaultValue = "0")
    val restoreTunnelOnBoot: Boolean = false,
    @ColumnInfo(name = "use_system_colors", defaultValue = "0")
    val useSystemColors: Boolean = false,
    @ColumnInfo(name = "tunnel_mode", defaultValue = "0") val tunnelMode: Int = 0,
    @ColumnInfo(name = "seamless_recovery", defaultValue = "1")
    val seamlessRecoveryEnabled: Boolean = true,
    @ColumnInfo(name = "seamless_recovery_bounce_delay_sec", defaultValue = "30")
    val seamlessRecoveryBounceDelaySec: Int = 30,
    @ColumnInfo(name = "global_amnezia_enabled", defaultValue = "0")
    val isGlobalAmneziaEnabled: Boolean = false,
    @ColumnInfo(name = "last_notified_update_version") val lastNotifiedUpdateVersion: String? = null,
)

package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.zaneschepke.wireguardautotunnel.client.data.converter.StringListConverter

@Entity(tableName = "auto_tunnel_settings")
data class AutoTunnelSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "is_tunnel_enabled", defaultValue = "0")
    val isAutoTunnelEnabled: Boolean = false,
    @field:ColumnTypeConverters(StringListConverter::class)
    @ColumnInfo(name = "trusted_network_ssids", defaultValue = "[]")
    val trustedNetworkSsids: List<String> = emptyList(),
    @ColumnInfo(name = "is_tunnel_on_ethernet_enabled", defaultValue = "0")
    val isTunnelOnEthernetEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_wifi_enabled", defaultValue = "0")
    val isTunnelOnWifiEnabled: Boolean = false,
    @ColumnInfo(name = "is_wildcards_enabled", defaultValue = "0")
    val isWildcardsEnabled: Boolean = false,
    @ColumnInfo(name = "is_stop_on_no_internet_enabled", defaultValue = "0")
    val isStopOnNoInternetEnabled: Boolean = false,
    @ColumnInfo(name = "start_on_boot", defaultValue = "0") val startOnBoot: Boolean = false,
    @field:ColumnTypeConverters(StringListConverter::class)
    @ColumnInfo(name = "trusted_network_bssids", defaultValue = "[]")
    val trustedNetworkBssids: List<String> = emptyList(),
)

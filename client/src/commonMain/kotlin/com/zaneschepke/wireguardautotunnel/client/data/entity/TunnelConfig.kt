package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.converter.StringListConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField

@Entity(tableName = "tunnel_config", indices = [Index(value = ["name"], unique = true)])
data class TunnelConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @field:ColumnTypeConverters(AppKeyringConverter::class)
    @ColumnInfo(name = "quick_config")
    val quickConfig: EncryptedField,
    @ColumnInfo(name = "is_primary_tunnel", defaultValue = "0")
    val isPrimaryTunnel: Boolean = false,
    @ColumnInfo(name = "prefer_ipv6", defaultValue = "0") val preferIpv6: Boolean = false,
    @ColumnInfo(name = "ipv6_restore_enabled", defaultValue = "0")
    val ipv6RestoreEnabled: Boolean = false,
    @ColumnInfo(name = "is_ddns_tunnel", defaultValue = "0") val isDdnsTunnel: Boolean = false,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
    @field:ColumnTypeConverters(StringListConverter::class)
    @ColumnInfo(name = "tunnel_networks", defaultValue = "[]")
    val tunnelNetworks: List<String> = emptyList(),
    @ColumnInfo(name = "is_ethernet_tunnel", defaultValue = "0")
    val isEthernetTunnel: Boolean = false,
    @field:ColumnTypeConverters(StringListConverter::class)
    @ColumnInfo(name = "tunnel_bssids", defaultValue = "[]")
    val tunnelBssids: List<String> = emptyList(),
)

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
    @ColumnInfo(name = "quick_config") val quickConfig: EncryptedField,
    @ColumnInfo(name = "tunnel_networks", defaultValue = "")
    val tunnelNetworks: Set<String> = setOf(),
    @ColumnInfo(name = "is_primary_tunnel", defaultValue = "false")
    val isPrimaryTunnel: Boolean = false,
    @ColumnInfo(name = "active", defaultValue = "false") val active: Boolean = false,
    @ColumnInfo(name = "ping_target", defaultValue = "null") var pingTarget: String? = null,
    @ColumnInfo(name = "is_ethernet_tunnel", defaultValue = "false")
    val isEthernetTunnel: Boolean = false,
    @ColumnInfo(name = "is_ipv4_preferred", defaultValue = "true")
    val isIpv4Preferred: Boolean = true,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
) {
    companion object {
        const val GLOBAL_CONFIG_NAME = "4675ab06-903a-438b-8485-6ea4187a9512"
    }
}

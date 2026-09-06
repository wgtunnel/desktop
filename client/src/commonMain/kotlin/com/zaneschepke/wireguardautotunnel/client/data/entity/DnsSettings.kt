package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "dns_settings")
data class DnsSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dns_bootstrap_protocol", defaultValue = "0")
    val bootstrapDnsProtocol: Int = 0,
    @ColumnInfo(name = "dns_bootstrap_endpoint") val bootstrapDnsEndpoint: String? = null,
    @ColumnInfo(name = "global_tunnel_config_dns_enabled", defaultValue = "0")
    val isGlobalTunnelConfigDnsEnabled: Boolean = false,
    @ColumnInfo(name = "tunnel_dns_mode", defaultValue = "0") val tunnelDnsMode: Int = 0,
    @ColumnInfo(name = "tunnel_dns_protocol", defaultValue = "0") val tunnelDnsProtocol: Int = 0,
    @ColumnInfo(name = "use_tunnel_dns_split", defaultValue = "1")
    val useTunnelDnsServersInSplit: Boolean = true,
    @ColumnInfo(name = "tunnel_dns_endpoint") val tunnelDnsEndpoint: String? = null,
    @ColumnInfo(name = "local_suffixes") val localSuffixes: String? = null,
    @ColumnInfo(name = "transit_dns_policy", defaultValue = "0") val transitDnsPolicy: Int = 0,
    @ColumnInfo(name = "split_suffix_target", defaultValue = "0") val splitSuffixTarget: Int = 0,
)

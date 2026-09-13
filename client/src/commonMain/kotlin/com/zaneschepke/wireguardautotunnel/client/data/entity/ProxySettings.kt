package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.zaneschepke.wireguardautotunnel.client.data.converter.AppKeyringConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField

@Entity(tableName = "proxy_settings")
data class ProxySettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "socks5_proxy_enabled", defaultValue = "1")
    val socks5ProxyEnabled: Boolean = true,
    @ColumnInfo(name = "socks5_proxy_bind_address") val socks5ProxyBindAddress: String? = null,
    @ColumnInfo(name = "http_proxy_enable", defaultValue = "1")
    val httpProxyEnabled: Boolean = true,
    @ColumnInfo(name = "http_proxy_bind_address") val httpProxyBindAddress: String? = null,
    @field:ColumnTypeConverters(AppKeyringConverter::class)
    @ColumnInfo(name = "proxy_username")
    val proxyUsername: EncryptedField? = null,
    @field:ColumnTypeConverters(AppKeyringConverter::class)
    @ColumnInfo(name = "proxy_password")
    val proxyPassword: EncryptedField? = null,
)

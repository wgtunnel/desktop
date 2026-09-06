package com.zaneschepke.wireguardautotunnel.client.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "monitoring_settings")
data class MonitoringSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "is_local_logs_enabled", defaultValue = "0")
    val isLocalLogsEnabled: Boolean = false,
    @ColumnInfo(name = "tunnel_statistics_enabled", defaultValue = "1")
    val tunnelStatisticsEnabled: Boolean = true,
    @ColumnInfo(name = "tunnel_statistics_poll_interval", defaultValue = "3")
    val tunnelStatisticsPollInterval: Int = 3,
)

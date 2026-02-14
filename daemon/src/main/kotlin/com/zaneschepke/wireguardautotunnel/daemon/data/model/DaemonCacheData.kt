package com.zaneschepke.wireguardautotunnel.daemon.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DaemonCacheData(
    val killSwitch: KillSwitchSettings = KillSwitchSettings(false, false),
    val startConfigs: Set<String> = emptySet(),
)

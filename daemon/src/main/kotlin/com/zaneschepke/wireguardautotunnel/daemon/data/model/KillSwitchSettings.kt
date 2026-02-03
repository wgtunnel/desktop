package com.zaneschepke.wireguardautotunnel.daemon.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KillSwitchSettings(val enabled: Boolean, val bypassLan: Boolean)

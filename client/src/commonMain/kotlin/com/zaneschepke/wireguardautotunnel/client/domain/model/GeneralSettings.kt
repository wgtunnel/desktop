package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.zaneschepke.wireguardautotunnel.client.data.model.AppMode
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    val id: Int = 0,
    val isRestoreOnBootEnabled: Boolean = false,
    val appMode: AppMode = AppMode.fromValue(0),
    val theme: Theme = Theme.AUTOMATIC,
    val locale: String? = null,
    val alreadyDonated: Boolean = false,
)

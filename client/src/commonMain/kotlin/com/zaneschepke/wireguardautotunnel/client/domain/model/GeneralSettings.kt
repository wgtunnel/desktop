package com.zaneschepke.wireguardautotunnel.client.domain.model

import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import kotlinx.serialization.Serializable

@Serializable
data class GeneralSettings(
    val id: Long = 1L,
    val theme: Theme = Theme.DARK,
    val locale: String? = null,
    val alreadyDonated: Boolean = false,
)

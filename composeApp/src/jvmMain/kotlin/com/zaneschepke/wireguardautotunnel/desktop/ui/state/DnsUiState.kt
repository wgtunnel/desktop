package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.wgtunnel.backend.model.dns.DnsValidationError
import com.zaneschepke.wireguardautotunnel.client.domain.model.DnsSettings

data class DnsUiState(
    val isLoaded: Boolean = false,
    val draft: DnsSettings = DnsSettings(),
    val saved: DnsSettings = DnsSettings(),
    val bootstrapEndpointError: DnsValidationError? = null,
    val tunnelEndpointError: DnsValidationError? = null,
    val localSuffixesError: DnsValidationError? = null,
    val hasActiveTunnel: Boolean = false,
) {
    val isDirty: Boolean
        get() = draft != saved
}

package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.wgtunnel.parser.ConfigQuickInclude
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig

data class GlobalConfigUiState(
    val isLoaded: Boolean = false,
    val stored: TunnelConfig = TunnelConfig.Empty,
    val editorText: String = "",
    val dnsEnabled: Boolean = false,
    val amneziaEnabled: Boolean = false,
    val storedDnsEnabled: Boolean = false,
    val storedAmneziaEnabled: Boolean = false,
) {
    val showEditor: Boolean
        get() = dnsEnabled || amneziaEnabled

    val isDirty: Boolean
        get() {
            if (dnsEnabled != storedDnsEnabled || amneziaEnabled != storedAmneziaEnabled) {
                return true
            }
            return editorText.trim() != expectedEditorText()
        }

    fun expectedEditorText(): String {
        if (!showEditor) return ""
        val config = runCatching { stored.asConfig() }.getOrNull() ?: return editorText
        return config.asQuickString(ConfigQuickInclude.global(dnsEnabled, amneziaEnabled))
    }
}

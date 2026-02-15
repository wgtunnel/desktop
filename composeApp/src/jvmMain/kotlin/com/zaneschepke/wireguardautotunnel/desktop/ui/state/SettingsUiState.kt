package com.zaneschepke.wireguardautotunnel.desktop.ui.state

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val lockdownEnabled: Boolean = false,
    val lockdownRestoreOnBootEnabled: Boolean = false,
    val lockdownBypassEnabled: Boolean = false,
    val tunnelRestoreOnBootEnabled: Boolean = false,
)

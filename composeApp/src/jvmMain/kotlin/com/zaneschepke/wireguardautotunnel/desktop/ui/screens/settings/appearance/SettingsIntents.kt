package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance

sealed class LockdownIntent {
    data class ToggleMaster(val enabled: Boolean) : LockdownIntent()

    data class TogglePersist(val enabled: Boolean) : LockdownIntent()

    data class ToggleBypassLan(val enabled: Boolean) : LockdownIntent()
}

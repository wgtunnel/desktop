package com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects

import com.dokar.sonner.ToastType

sealed class AppSideEffect {
    data class Toast(val message: String, val type: ToastType) : AppSideEffect()

    data class ActionableToast(
        val id: String,
        val message: String,
        val copyText: String,
        val copyLabel: String,
        val type: ToastType = ToastType.Warning,
    ) : AppSideEffect()

    // Dismissable, with an action that navigates to Support to install update,
    // shown at most once per version
    data class UpdateAvailableToast(val id: String, val message: String) : AppSideEffect()

    data class DismissToast(val id: String) : AppSideEffect()
}

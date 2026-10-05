package com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects

import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import kotlin.time.Duration

sealed class AppSideEffect {
    data class Toast(val message: String, val type: ToastType) : AppSideEffect()

    data class Navigate(val route: Route) : AppSideEffect()

    data class ActionableToast(
        val id: String,
        val message: String,
        val copyText: String,
        val copyLabel: String,
        val type: ToastType = ToastType.Warning,
    ) : AppSideEffect()

    data class NavigableToast(
        val id: String,
        val message: String,
        val actionLabel: String,
        val onAction: () -> Unit,
        val type: ToastType = ToastType.Warning,
        val duration: Duration = Duration.INFINITE,
    ) : AppSideEffect()

    data class TextActionToast(
        val id: String,
        val message: String,
        val actionLabel: String,
        val onAction: () -> Unit,
        val type: ToastType = ToastType.Normal,
        val duration: Duration = Duration.INFINITE,
    ) : AppSideEffect()

    data class DismissToast(val id: String) : AppSideEffect()
}

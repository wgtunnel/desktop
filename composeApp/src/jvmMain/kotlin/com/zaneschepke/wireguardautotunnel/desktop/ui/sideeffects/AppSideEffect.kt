package com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects

import com.dokar.sonner.ToastType

sealed class AppSideEffect {
    data class Toast(val message: String, val type: ToastType) : AppSideEffect()
}

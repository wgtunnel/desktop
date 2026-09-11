package com.zaneschepke.wireguardautotunnel.desktop.ui.common.toast

import com.dokar.sonner.Toast

data class CommandToastMessage(val description: String, val command: String)
class CopyCommandAction(val contentDescription: String, val onClick: (Toast) -> Unit)

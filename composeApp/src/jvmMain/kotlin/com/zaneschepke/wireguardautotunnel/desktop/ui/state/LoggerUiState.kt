package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.LogMessageDto

data class LoggerUiState(
    val isLoading: Boolean = true,
    val messages: List<LogMessageDto> = emptyList(),
)

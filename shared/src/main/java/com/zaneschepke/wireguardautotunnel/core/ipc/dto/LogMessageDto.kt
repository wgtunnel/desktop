package com.zaneschepke.wireguardautotunnel.core.ipc.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogMessageDto(
    val time: String,
    val level: String,
    val tag: String,
    val message: String,
    val source: String,
) {
    override fun toString(): String = "$time $level $source/$tag $message"
}

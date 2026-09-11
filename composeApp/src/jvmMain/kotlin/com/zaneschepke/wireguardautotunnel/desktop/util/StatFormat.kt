package com.zaneschepke.wireguardautotunnel.desktop.util

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import nl.jacobras.humanreadable.HumanReadable

fun Long.formatFileSize(): String = HumanReadable.fileSize(this, decimals = 1)

fun Long?.toAgoDisplay(currentTimeMillis: Long = System.currentTimeMillis()): String? {
    val timestamp = this ?: return null
    if (timestamp <= 0L) return null
    val nowSeconds = currentTimeMillis / 1000
    val secondsAgo = (nowSeconds - timestamp).coerceAtLeast(0L)
    return HumanReadable.duration(secondsAgo.seconds) + " ago"
}

fun Long?.toUptimeDisplay(currentTimeMillis: Long = System.currentTimeMillis()): String? {
    val startedAt = this?.takeIf { it > 0L } ?: return null
    return HumanReadable.duration((currentTimeMillis - startedAt).coerceAtLeast(0L).milliseconds)
}

fun String.abbreviateKey(prefixLength: Int = 6): String {
    if (length <= prefixLength * 2 + 1) return this
    return take(prefixLength) + "…" + takeLast(prefixLength)
}

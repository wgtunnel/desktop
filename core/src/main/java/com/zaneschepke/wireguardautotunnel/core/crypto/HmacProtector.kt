package com.zaneschepke.wireguardautotunnel.core.crypto

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.SecureCommand
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.math.abs

object HmacProtector {
    private const val ALGORITHM = "HmacSHA256"

    fun generateSignature(key: String, timestamp: Long, payload: String?): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(key.toByteArray(), ALGORITHM))
        val dataToSign = "$timestamp${payload ?: ""}"
        return Base64.encode(mac.doFinal(dataToSign.toByteArray()))
    }

    fun verify(key: String, command: SecureCommand): Boolean {
        val now = System.currentTimeMillis() / 1000
        // 30 seconds window to prevent replay attacks
        if (abs(now - command.timestamp) > 30) return false

        val expected = generateSignature(key, command.timestamp, command.payload)
        return expected == command.signature
    }
}
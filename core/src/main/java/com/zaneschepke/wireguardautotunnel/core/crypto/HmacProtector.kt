package com.zaneschepke.wireguardautotunnel.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.math.abs

object HmacProtector {
    private const val ALGORITHM = "HmacSHA256"

    /**
     * Generates a signature based on the secret key, a timestamp, and payload.
     */
    fun generateSignature(key: String, timestamp: Long, payload: String?): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(key.toByteArray(), ALGORITHM))
        val dataToSign = "$timestamp${payload ?: ""}"
        return Base64.encode(mac.doFinal(dataToSign.toByteArray()))
    }

    /**
     * Verifies the signature by comparing it with a freshly generated one.
     * Checks if the timestamp is within a 30-second window.
     */
    fun verify(key: String, timestamp: Long, signature: String, payload: String?): Boolean {
        val now = System.currentTimeMillis() / 1000

        if (abs(now - timestamp) > 30) return false

        val expected = generateSignature(key, timestamp, payload)

        return expected == signature
    }
}
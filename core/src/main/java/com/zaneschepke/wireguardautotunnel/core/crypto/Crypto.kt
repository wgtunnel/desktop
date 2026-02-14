package com.zaneschepke.wireguardautotunnel.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

object Crypto {

    const val KEY_ALGORITHM = "AES"
    const val CYPHER = "AES/GCM/NoPadding"

    private val random = SecureRandom()

    fun generateRandomBase64(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        random.nextBytes(bytes)
        return Base64.encode(bytes)
    }

    fun generateRandomAESKey(): SecretKey {
        val keyBytes = ByteArray(32)
        random.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    fun generateRandomBase64EncodedAesKey(): String {
        return Base64.encode(generateRandomAESKey().encoded)
    }

    fun decodeKey(key: String): SecretKey {
        return SecretKeySpec(Base64.decode(key), KEY_ALGORITHM)
    }

    fun encryptWithMasterKey(plainText: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(CYPHER)
        val iv = ByteArray(12) // 96-bit IV for GCM
        random.nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // store IV + ciphertext together, base64-encoded
        val combined = iv + cipherText
        return Base64.encode(combined)
    }

    fun decryptWithMasterKey(encrypted: String, key: SecretKey): String {
        val combined = Base64.decode(encrypted)
        val iv = combined.copyOfRange(0, 12)
        val cipherText = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance(CYPHER)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }
}

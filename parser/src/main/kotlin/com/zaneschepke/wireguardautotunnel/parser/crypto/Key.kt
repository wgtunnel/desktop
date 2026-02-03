// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 WG Tunnel.
// Adapted from WireGuard LLC.

package com.zaneschepke.wireguardautotunnel.parser.crypto

import io.github.andreypfau.curve25519.x25519.X25519
import org.kotlincrypto.random.CryptoRand
import kotlin.experimental.and
import kotlin.experimental.or

class KeyFormatException : Exception {
    constructor(format: Key.Format, type: Key.Type) : super("Invalid key format: $format, type: $type")
}

class Key private constructor(private val key: ByteArray) {

    fun getBytes(): ByteArray = key.copyOf()

    fun toBase64(): String {
        val output = CharArray(Format.BASE64.length)
        var i = 0
        while (i < key.size / 3) {
            encodeBase64(key, i * 3, output, i * 4)
            i++
        }
        val endSegment = byteArrayOf(key[i * 3], key[i * 3 + 1], 0)
        encodeBase64(endSegment, 0, output, i * 4)
        output[Format.BASE64.length - 1] = '='
        return output.concatToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Key) return false
        return key.contentEquals(other.key)
    }

    override fun hashCode(): Int {
        var ret = 0
        var i = 0
        while (i < key.size / 4) {
            ret = ret xor ((key[i * 4 + 0].toInt() shr 0) + (key[i * 4 + 1].toInt() shr 8) +
                    (key[i * 4 + 2].toInt() shr 16) + (key[i * 4 + 3].toInt() shr 24))
            i++
        }
        return ret
    }

    companion object {
        fun fromBase64(str: String): Key {
            val input = str.toCharArray()
            if (input.size != Format.BASE64.length || input[Format.BASE64.length - 1] != '=') {
                throw KeyFormatException(Format.BASE64, Type.LENGTH)
            }
            val key = ByteArray(Format.BINARY.length)
            var ret = 0
            var i = 0
            while (i < key.size / 3) {
                val value = decodeBase64(input, i * 4)
                ret = ret or (value ushr 31)
                key[i * 3] = ((value ushr 16) and 0xff).toByte()
                key[i * 3 + 1] = ((value ushr 8) and 0xff).toByte()
                key[i * 3 + 2] = (value and 0xff).toByte()
                i++
            }
            val endSegment = charArrayOf(input[i * 4], input[i * 4 + 1], input[i * 4 + 2], 'A')
            val value = decodeBase64(endSegment, 0)
            ret = ret or ((value ushr 31) or (value and 0xff))
            key[i * 3] = ((value ushr 16) and 0xff).toByte()
            key[i * 3 + 1] = ((value ushr 8) and 0xff).toByte()

            if (ret != 0) {
                throw KeyFormatException(Format.BASE64, Type.CONTENTS)
            }
            return Key(key)
        }

        fun fromBytes(bytes: ByteArray): Key {
            if (bytes.size != Format.BINARY.length) {
                throw KeyFormatException(Format.BINARY, Type.LENGTH)
            }
            return Key(bytes)
        }

        fun generatePrivateKey(): Key {
            val privateKey = ByteArray(Format.BINARY.length)
            CryptoRand.nextBytes(privateKey)
            privateKey[0] = privateKey[0] and 248.toByte()
            privateKey[31] = privateKey[31] and 127.toByte()
            privateKey[31] = privateKey[31] or 64.toByte()
            return Key(privateKey)
        }

        fun generatePublicKey(privateKey: Key): Key {
            val publicKey = ByteArray(Format.BINARY.length)
            X25519.x25519(privateKey.getBytes(), output = publicKey)
            return Key(publicKey)
        }

        private fun decodeBase64(src: CharArray, srcOffset: Int): Int {
            var value = 0
            for (i in 0 until 4) {
                val c = src[i + srcOffset].code
                value = value or (-1 +
                        ((((('A'.code - 1) - c) and (c - ('Z'.code + 1))) ushr 8) and (c - 64)) +
                        ((((('a'.code - 1) - c) and (c - ('z'.code + 1))) ushr 8) and (c - 70)) +
                        ((((('0'.code - 1) - c) and (c - ('9'.code + 1))) ushr 8) and (c + 5)) +
                        (((('+'.code - 1) - c) and (c - ('+'.code + 1))) ushr 8 and 63) +
                        (((('/'.code - 1) - c) and (c - ('/'.code + 1))) ushr 8 and 64)
                        ) shl (18 - 6 * i)
            }
            return value
        }

        private fun encodeBase64(src: ByteArray, srcOffset: Int, dest: CharArray, destOffset: Int) {
            val input = byteArrayOf(
                (src[srcOffset].toInt() shr 2 and 63).toByte(),
                ((src[srcOffset].toInt() shl 4 or (src[1 + srcOffset].toInt() and 0xff ushr 4)) and 63).toByte(),
                ((src[1 + srcOffset].toInt() shl 2 or (src[2 + srcOffset].toInt() and 0xff ushr 6)) and 63).toByte(),
                (src[2 + srcOffset].toInt() and 63).toByte()
            )
            for (i in 0 until 4) {
                dest[i + destOffset] = (input[i].toInt() + 'A'.code +
                        (((25 - input[i].toInt()) ushr 8) and 6) -
                        (((51 - input[i].toInt()) ushr 8) and 75) -
                        (((61 - input[i].toInt()) ushr 8) and 15) +
                        (((62 - input[i].toInt()) ushr 8) and 3)).toChar()
            }
        }
    }

    enum class Format(val length: Int) {
        BASE64(44),
        BINARY(32),
        HEX(64)
    }

    enum class Type {
        LENGTH,
        CONTENTS
    }
}
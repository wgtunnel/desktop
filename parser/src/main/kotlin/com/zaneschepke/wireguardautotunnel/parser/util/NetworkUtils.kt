package com.zaneschepke.wireguardautotunnel.parser.util

import com.zaneschepke.wireguardautotunnel.parser.ConfigParseException
import com.zaneschepke.wireguardautotunnel.parser.ErrorType
import java.net.InetAddress
import org.apache.commons.validator.routines.InetAddressValidator

object NetworkUtils {

    private val validator = InetAddressValidator.getInstance()

    fun isValidIp(ip: String): Boolean {
        return validator.isValid(ip.removeSurrounding("[", "]"))
    }

    fun isValidCidr(cidr: String): Boolean {
        val parts = cidr.split("/", limit = 2)
        val ip = parts[0]

        if (parts.size == 1) {
            return isValidIp(ip)
        }

        val prefix = parts[1].toIntOrNull() ?: return false
        if (!isValidIp(ip)) return false

        return try {
            val addr = InetAddress.getByName(ip.removeSurrounding("[", "]"))
            val maxPrefix = if (addr is java.net.Inet4Address) 32 else 128
            prefix in 0..maxPrefix
        } catch (e: Exception) {
            false
        }
    }

    fun isValidDnsEntry(entry: String): Boolean {
        if (entry.isBlank()) return false
        return isValidIp(entry) || isValidHostname(entry)
    }

    fun isValidHostname(host: String): Boolean {
        val cleaned = host.removeSurrounding("[", "]").trim()
        if (cleaned.isBlank() || cleaned.length > 253) return false
        if (cleaned.startsWith(".") || cleaned.endsWith(".") || cleaned.contains("..")) return false

        return cleaned.split('.').all { label ->
            label.length in 1..63 &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.matches(Regex("^[a-zA-Z0-9-]+$"))
        }
    }

    fun isValidBase64(str: String): Boolean {
        return try {
            val decoded = kotlin.io.encoding.Base64.decode(str)
            decoded.size == 32
        } catch (_: Exception) {
            false
        }
    }

    fun isValidAmneziaHeader(header: String): Boolean {
        val maxUInt32 = 4294967295L
        return try {
            if (header.contains("-")) {
                val parts = header.split("-")
                if (parts.size != 2) return false
                val start = parts[0].trim().toLong()
                val end = parts[1].trim().toLong()
                start in 0..maxUInt32 && end in 0..maxUInt32 && start <= end
            } else {
                header.trim().toLong() in 0..maxUInt32
            }
        } catch (_: Exception) {
            false
        }
    }

    @Throws(ConfigParseException::class)
    fun validateAmneziaSignaturePacket(value: String, fieldName: String) {
        if (value.isBlank()) {
            throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
        }

        var index = 0

        // every tag mush start with <
        while (index < value.length) {
            if (value[index] != '<') {
                throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
            }
            index++

            val typeStart = index
            while (index < value.length && value[index].isLetter()) {
                index++
            }
            val tagType = value.substring(typeStart, index).lowercase()

            if (tagType.isEmpty()) {
                throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
            }

            // All tags except <t> require a space
            if (tagType != "t") {
                if (index >= value.length || value[index] != ' ') {
                    throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
                }
                index++
            }

            when (tagType) {
                "b" -> index = parseStaticBytesTag(value, index, fieldName)
                "r",
                "rd",
                "rc" -> index = parseRandomTag(value, index, fieldName)
                "t" -> {} // timestamp has no parameter
                else ->
                    throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
            }

            // every tag must end with >
            if (index >= value.length || value[index] != '>') {
                throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
            }
            index++
        }
    }

    private fun parseStaticBytesTag(value: String, start: Int, fieldName: String): Int {
        var index = start

        // must start with 0x
        if (
            index + 2 > value.length ||
                !value.substring(index, index + 2).equals("0x", ignoreCase = true)
        ) {
            throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
        }
        index += 2

        val hexStart = index
        while (index < value.length && value[index].isHexDigit()) {
            index++
        }

        // must be a valid hex
        val hexLength = index - hexStart
        if (hexLength == 0 || hexLength % 2 != 0) {
            throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
        }
        return index
    }

    private fun parseRandomTag(value: String, start: Int, fieldName: String): Int {
        var index = start
        val numStart = index
        while (index < value.length && value[index].isDigit()) {
            index++
        }

        // must have at least one digit
        if (index == numStart) {
            throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
        }

        // make sure it is a positive number
        val size = value.substring(numStart, index).toIntOrNull()
        if (size == null || size <= 0) {
            throw ConfigParseException(ErrorType.INVALID_SIGNATURE_FORMAT, fieldName, value)
        }
        return index
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}

package com.zaneschepke.wireguardautotunnel.parser.util

import java.net.InetAddress

object NetworkUtils {
    private val hostnameRegex = Regex("^(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])*$")


    fun isValidIp(ip: String): Boolean {
        val sanitized = ip.removeSurrounding("[", "]")
        if (sanitized.any { it.lowercaseChar() in 'g'..'z' }) return false

        return try {
            InetAddress.getAllByName(sanitized).isNotEmpty()
        } catch (e: Exception) {
            false
        }
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
        // Safe: isValidIp is offline, isValidHostname is regex.
        return isValidIp(entry) || isValidHostname(entry)
    }


    fun isValidHostname(host: String): Boolean {
        val cleaned = host.removeSurrounding("[", "]")
        return hostnameRegex.matches(cleaned) && cleaned.length <= 253
    }

    fun isValidBase64(str: String): Boolean {
        // WireGuard keys are always 44 chars (32 bytes encoded)
        if (str.length != 44 || !str.endsWith("=")) return false
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        return str.all { it in base64Chars }
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

    fun isValidHexSignature(signature: String): Boolean {
        val hex = signature.removePrefix("0x").trim()
        if (hex.isEmpty() || hex.length % 2 != 0) return false
        val hexChars = "0123456789abcdefABCDEF"
        return hex.all { it in hexChars }
    }
}
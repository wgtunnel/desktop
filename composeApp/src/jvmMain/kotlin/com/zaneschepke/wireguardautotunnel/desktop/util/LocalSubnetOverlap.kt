package com.zaneschepke.wireguardautotunnel.desktop.util

import java.net.InetAddress
import java.net.NetworkInterface

// macOS keeps one route per destination, so an AllowedIPs entry that is exactly a local
// interface's subnet cannot be pointed at the tunnel and stays on that interface, as with
// wg-quick.
object LocalSubnetOverlap {

    private data class Subnet(val bytes: List<Byte>, val bits: Int)

    fun find(allowedIps: List<String>): List<String> = find(allowedIps, localSubnets())

    internal fun find(allowedIps: List<String>, local: List<Pair<InetAddress, Int>>): List<String> {
        val localSubnets = local.mapNotNull { (addr, bits) -> subnet(addr, bits) }.toSet()
        return allowedIps
            .map { it.trim() }
            .filter { entry ->
                val slash = entry.indexOf('/')
                if (slash <= 0) return@filter false
                val bits = entry.substring(slash + 1).toIntOrNull() ?: return@filter false
                val addr = parseLiteral(entry.substring(0, slash)) ?: return@filter false
                subnet(addr, bits)?.let { it.bits > 0 && it in localSubnets } == true
            }
            .distinct()
    }

    private fun localSubnets(): List<Pair<InetAddress, Int>> =
        runCatching {
                NetworkInterface.getNetworkInterfaces()
                    .toList()
                    .filter { it.isUp && !it.isLoopback && !it.name.startsWith("utun") }
                    .flatMap { iface ->
                        iface.interfaceAddresses.mapNotNull { ia ->
                            val addr = ia.address ?: return@mapNotNull null
                            if (addr.isLinkLocalAddress) null
                            else addr to ia.networkPrefixLength.toInt()
                        }
                    }
            }
            .getOrDefault(emptyList())

    private fun subnet(addr: InetAddress, bits: Int): Subnet? {
        val raw = addr.address
        if (bits < 0 || bits > raw.size * 8) return null
        val masked =
            raw.mapIndexed { i, b ->
                val keep = (bits - i * 8).coerceIn(0, 8)
                (b.toInt() and (0xFF shl (8 - keep)).and(0xFF)).toByte()
            }
        return Subnet(masked, bits)
    }

    // Only numeric literals, so InetAddress never does a DNS lookup.
    private fun parseLiteral(s: String): InetAddress? {
        val isV4 = s.count { it == '.' } == 3 && s.all { it.isDigit() || it == '.' }
        val isV6 = s.contains(':') && s.all { it.isLetterOrDigit() || it == ':' || it == '.' }
        if (!isV4 && !isV6) return null
        return runCatching { InetAddress.getByName(s) }.getOrNull()
    }
}

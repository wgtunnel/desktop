package com.zaneschepke.wireguardautotunnel.parser.util

import com.zaneschepke.wireguardautotunnel.parser.ActivePeer
import com.zaneschepke.wireguardautotunnel.parser.InterfaceSection
import com.zaneschepke.wireguardautotunnel.parser.PeerSection
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import nl.jacobras.humanreadable.HumanReadable

object ConfigFormatter {

    fun appendInterfaceSection(
        sb: StringBuilder,
        iface: InterfaceSection,
        hidePrivateKey: Boolean = false,
    ) {
        iface.comments.forEach { sb.appendLine(it) }
        sb.appendLine("[Interface]")
        sb.appendLine("PrivateKey = ${if (hidePrivateKey) "(hidden)" else iface.privateKey}")
        iface.address?.let { sb.appendLine("Address = $it") }
        iface.dns?.let { sb.appendLine("DNS = $it") }
        iface.listenPort?.let { sb.appendLine("ListenPort = $it") }
        iface.mtu?.let { sb.appendLine("MTU = $it") }
        iface.fwMark?.let { sb.appendLine("FwMark = $it") }
        iface.table?.let { sb.appendLine("Table = $it") }
        iface.saveConfig?.let { sb.appendLine("SaveConfig = $it") }

        // AmneziaWG
        iface.jC?.let { sb.appendLine("Jc = $it") }
        iface.jMin?.let { sb.appendLine("Jmin = $it") }
        iface.jMax?.let { sb.appendLine("Jmax = $it") }
        iface.s1?.let { sb.appendLine("S1 = $it") }
        iface.s2?.let { sb.appendLine("S2 = $it") }
        iface.s3?.let { sb.appendLine("S3 = $it") }
        iface.s4?.let { sb.appendLine("S4 = $it") }
        iface.h1?.let { sb.appendLine("H1 = $it") }
        iface.h2?.let { sb.appendLine("H2 = $it") }
        iface.h3?.let { sb.appendLine("H3 = $it") }
        iface.h4?.let { sb.appendLine("H4 = $it") }
        iface.i1?.let { sb.appendLine("I1 = $it") }
        iface.i2?.let { sb.appendLine("I2 = $it") }
        iface.i3?.let { sb.appendLine("I3 = $it") }
        iface.i4?.let { sb.appendLine("I4 = $it") }
        iface.i5?.let { sb.appendLine("I5 = $it") }

        iface.includedApplications?.let {
            sb.appendLine("IncludedApplications = ${it.joinToString(",")}")
        }
        iface.excludedApplications?.let {
            sb.appendLine("ExcludedApplications = ${it.joinToString(",")}")
        }
    }

    fun appendPeerSection(sb: StringBuilder, peer: PeerSection) {
        peer.comments.forEach { sb.appendLine(it) }
        sb.append("\n[Peer]\n")
        appendCommonPeerFields(
            sb,
            peer.publicKey,
            peer.endpoint,
            peer.allowedIPs,
            peer.presharedKey,
            peer.persistentKeepalive,
        )
    }

    fun appendActivePeerSection(sb: StringBuilder, peer: ActivePeer) {
        sb.append("\n[Peer]\n")
        appendCommonPeerFields(
            sb,
            peer.publicKey,
            peer.endpoint,
            peer.allowedIPs,
            peer.presharedKey,
            peer.persistentKeepalive,
        )
        appendRuntimeStats(sb, peer)
    }

    private fun appendCommonPeerFields(
        sb: StringBuilder,
        publicKey: String,
        endpoint: String?,
        allowedIPs: String?,
        presharedKey: String?,
        persistentKeepalive: Int?,
    ) {
        sb.appendLine("PublicKey = $publicKey")
        endpoint?.let { sb.appendLine("Endpoint = $it") }
        allowedIPs?.let { sb.appendLine("AllowedIPs = $it") }
        if (
            presharedKey != null && presharedKey != "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        ) {
            sb.appendLine("PresharedKey = $presharedKey")
        }
        if (persistentKeepalive != null && persistentKeepalive != 0) {
            sb.appendLine("PersistentKeepalive = $persistentKeepalive")
        }
    }

    private fun appendRuntimeStats(sb: StringBuilder, peer: ActivePeer) {
        peer.lastHandshakeSeconds?.let { seconds ->
            if (seconds == 0L) {
                sb.appendLine("LastHandshake =")
            } else {
                val handshakeInstant = Instant.fromEpochSeconds(seconds)
                val agoDuration = Clock.System.now() - handshakeInstant
                sb.appendLine("LastHandshake = ${agoDuration.toDetailedString()} ago")
            }
        }
        peer.txBytes?.let { sb.appendLine("TxBytes = ${HumanReadable.fileSize(it, decimals = 1)}") }
        peer.rxBytes?.let { sb.appendLine("RxBytes = ${HumanReadable.fileSize(it, decimals = 1)}") }
    }

    private fun Duration.toDetailedString(): String {
        val days = inWholeDays
        val hours = toComponents { _, h, _, _, _ -> h }
        val minutes = toComponents { _, _, m, _, _ -> m }
        val seconds = toComponents { _, _, _, s, _ -> s }

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minute${if (minutes > 1) "s" else ""}")
        if (seconds > 0) parts.add("$seconds second${if (seconds > 1) "s" else ""}")

        return if (parts.isEmpty()) "0 seconds" else parts.joinToString(" ")
    }
}

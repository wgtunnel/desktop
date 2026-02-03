package com.zaneschepke.wireguardautotunnel.parser

import com.zaneschepke.wireguardautotunnel.parser.crypto.Key
import com.zaneschepke.wireguardautotunnel.parser.util.getBool
import com.zaneschepke.wireguardautotunnel.parser.util.getInt
import com.zaneschepke.wireguardautotunnel.parser.util.getList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("Interface") val `interface`: InterfaceSection,
    @SerialName("Peer") val peers: List<PeerSection> = emptyList()
) {

    @Throws(ConfigParseException::class)
    fun validate() {
        `interface`.validate()
        peers.forEachIndexed { index, peer -> peer.validate(index) }
    }

    fun asQuickString(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${`interface`.privateKey}")
        `interface`.address?.let { appendLine("Address = $it") }
        `interface`.dns?.let { appendLine("DNS = $it") }
        `interface`.listenPort?.let { appendLine("ListenPort = $it") }
        `interface`.mtu?.let { appendLine("MTU = $it") }
        `interface`.fwMark?.let { appendLine("FwMark = $it") }
        `interface`.table?.let { appendLine("Table = $it") }
        `interface`.saveConfig?.let { appendLine("SaveConfig = $it") }

        // AmneziaWG
        `interface`.jC?.let { appendLine("Jc = $it") }
        `interface`.jMin?.let { appendLine("Jmin = $it") }
        `interface`.jMax?.let { appendLine("Jmax = $it") }
        `interface`.s1?.let { appendLine("S1 = $it") }
        `interface`.s2?.let { appendLine("S2 = $it") }
        `interface`.s3?.let { appendLine("S3 = $it") }
        `interface`.s4?.let { appendLine("S4 = $it") }
        `interface`.h1?.let { appendLine("H1 = $it") }
        `interface`.h2?.let { appendLine("H2 = $it") }
        `interface`.h3?.let { appendLine("H3 = $it") }
        `interface`.h4?.let { appendLine("H4 = $it") }
        `interface`.i1?.let { appendLine("I1 = $it") }
        `interface`.i2?.let { appendLine("I2 = $it") }
        `interface`.i3?.let { appendLine("I3 = $it") }
        `interface`.i4?.let { appendLine("I4 = $it") }
        `interface`.i5?.let { appendLine("I5 = $it") }

        `interface`.includedApplications?.let { appendLine("IncludedApplications = ${it.joinToString(",")}") }
        `interface`.excludedApplications?.let { appendLine("ExcludedApplications = ${it.joinToString(",")}") }

        peers.forEach { peer ->
            append("\n[Peer]\n")
            appendLine("PublicKey = ${peer.publicKey}")
            peer.endpoint?.let { appendLine("Endpoint = $it") }
            peer.allowedIPs?.let { appendLine("AllowedIPs = $it") }
            peer.presharedKey?.let { appendLine("PresharedKey = $it") }
            peer.persistentKeepalive?.let { appendLine("PersistentKeepalive = $it") }
        }
    }.trim()

    fun rotateInterfaceKey(): Config {
        val privateKey = Key.generatePrivateKey()
        val newInterface = `interface`.copy(privateKey = privateKey.toBase64())
        return copy(`interface` = newInterface)
    }

    companion object {
        @Throws(ConfigParseException::class)
        fun parseQuickString(configString: String): Config {
            val interfaceMap = mutableMapOf<String, String>()
            val peerMaps = mutableListOf<MutableMap<String, String>>()
            var currentSection: MutableMap<String, String>? = null

            configString.lines().forEach { line ->
                val trimmed = line.split("#", ";")[0].trim()
                if (trimmed.isEmpty()) return@forEach

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = when (trimmed.substring(1, trimmed.length - 1).lowercase()) {
                        "interface" -> interfaceMap
                        "peer" -> mutableMapOf<String, String>().also { peerMaps.add(it) }
                        else -> null // ignore unknown
                    }
                    return@forEach
                }

                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    currentSection?.put(parts[0].trim(), parts[1].trim())
                }
            }

            if (interfaceMap.isEmpty()) throw ConfigParseException(ErrorType.MISSING_REQUIRED_FIELD, "Interface")

            return Config(
                `interface` = buildInterface(interfaceMap),
                peers = peerMaps.map { buildPeer(it) }
            ).also { it.validate() }
        }

        private fun buildInterface(m: Map<String, String>) = InterfaceSection(
            privateKey = m["PrivateKey"] ?: "",
            address = m["Address"],
            dns = m["DNS"],
            listenPort = m.getInt("ListenPort", "Interface"),
            mtu = m.getInt("MTU", "Interface"),
            fwMark = m.getInt("FwMark", "Interface"),
            table = m["Table"],
            saveConfig = m.getBool("SaveConfig", "Interface"),
            jC = m.getInt("Jc", "Interface"),
            jMin = m.getInt("Jmin", "Interface"),
            jMax = m.getInt("Jmax", "Interface"),
            s1 = m.getInt("S1", "Interface"),
            s2 = m.getInt("S2", "Interface"),
            s3 = m.getInt("S3", "Interface"),
            s4 = m.getInt("S4", "Interface"),
            h1 = m["H1"], h2 = m["H2"], h3 = m["H3"], h4 = m["H4"],
            i1 = m["I1"], i2 = m["I2"], i3 = m["I3"], i4 = m["I4"], i5 = m["I5"],
            includedApplications = m.getList("IncludedApplications"),
            excludedApplications = m.getList("ExcludedApplications")
        )

        private fun buildPeer(m: Map<String, String>) = PeerSection(
            publicKey = m["PublicKey"] ?: "",
            allowedIPs = m["AllowedIPs"],
            endpoint = m["Endpoint"],
            presharedKey = m["PresharedKey"],
            persistentKeepalive = m.getInt("PersistentKeepalive", "Peer")
        )

        fun parseEndpoint(endpoint: String): Pair<String?, String?> {
            var host: String
            var portStr: String?
            if (endpoint.startsWith("[")) {
                val endBracket = endpoint.lastIndexOf("]")
                if (endBracket == -1 || !endpoint.substring(endBracket + 1).startsWith(":")) return null to null
                host = endpoint.take(endBracket + 1)
                portStr = endpoint.substring(endBracket + 2)
            } else {
                val parts = endpoint.split(":", limit = 2)
                if (parts.size != 2) return null to null
                host = parts[0]
                portStr = parts[1]
            }
            return host to portStr
        }

        internal fun generatePublicKeyFromPrivate(privateBase64: String): String {
            val privateKey = Key.fromBase64(privateBase64)
            val publicKey = Key.generatePublicKey(privateKey)
            return publicKey.toBase64()
        }
    }
}
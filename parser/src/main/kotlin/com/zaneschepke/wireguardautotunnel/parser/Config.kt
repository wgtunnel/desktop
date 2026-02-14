package com.zaneschepke.wireguardautotunnel.parser

import com.zaneschepke.wireguardautotunnel.parser.crypto.Key
import com.zaneschepke.wireguardautotunnel.parser.util.ConfigFormatter
import com.zaneschepke.wireguardautotunnel.parser.util.getBool
import com.zaneschepke.wireguardautotunnel.parser.util.getInt
import com.zaneschepke.wireguardautotunnel.parser.util.getList
import com.zaneschepke.wireguardautotunnel.parser.util.getLong
import kotlin.io.encoding.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("Interface") val `interface`: InterfaceSection,
    @SerialName("Peer") val peers: List<PeerSection> = emptyList(),
    val headerComments: List<String> = emptyList(),
) {

    @Throws(ConfigParseException::class)
    fun validate() {
        `interface`.validate()
        peers.forEachIndexed { index, peer -> peer.validate(index) }
    }

    fun asQuickString(): String =
        buildString {
                headerComments.forEach { appendLine(it) }
                ConfigFormatter.appendInterfaceSection(this, `interface`)
                peers.forEach { ConfigFormatter.appendPeerSection(this, it) }
            }
            .trim()

    fun rotateInterfaceKey(): Config {
        val privateKey = Key.generatePrivateKey()
        val newInterface = `interface`.copy(privateKey = privateKey.toBase64())
        return copy(`interface` = newInterface)
    }

    companion object {
        fun parseQuickString(configString: String): Config {
            val interfaceMap = mutableMapOf<String, String>()
            val peerMaps = mutableListOf<Pair<MutableMap<String, String>, List<String>>>()

            val headerComments = mutableListOf<String>()
            val currentCommentBuffer = mutableListOf<String>()
            var interfaceComments = listOf<String>()

            var currentSectionMap: MutableMap<String, String>? = null
            var isFirstSectionFound = false

            // normalize and trim
            val normalizedConfig = configString.replace("\r\n", "\n").replace("\r", "\n").trim()

            normalizedConfig.lines().forEach { line ->
                val raw = line.trim()
                if (raw.isEmpty()) return@forEach

                // handle comments
                if (raw.startsWith("#") || raw.startsWith(";")) {
                    if (!isFirstSectionFound) {
                        headerComments.add(raw)
                    } else {
                        currentCommentBuffer.add(raw)
                    }
                    return@forEach
                }

                // Handle Section Headers
                if (raw.startsWith("[") && raw.endsWith("]")) {
                    isFirstSectionFound = true
                    val sectionName = raw.substring(1, raw.length - 1).lowercase()

                    when (sectionName) {
                        "interface" -> {
                            currentSectionMap = interfaceMap
                            interfaceComments = currentCommentBuffer.toList()
                            currentCommentBuffer.clear()
                        }
                        "peer" -> {
                            val newPeerMap = mutableMapOf<String, String>()
                            peerMaps.add(newPeerMap to currentCommentBuffer.toList())
                            currentSectionMap = newPeerMap
                            currentCommentBuffer.clear()
                        }
                        else -> currentSectionMap = null
                    }
                    return@forEach
                }

                val parts = raw.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    // remove whitespaces
                    if (
                        key in
                            listOf(
                                "PrivateKey",
                                "PublicKey",
                                "PresharedKey",
                                "H1",
                                "H2",
                                "H3",
                                "H4",
                            )
                    ) {
                        value = value.replace(Regex("\\s+"), "")
                    }
                    currentSectionMap?.put(key, value)
                }
            }

            return Config(
                headerComments = headerComments,
                `interface` = buildInterface(interfaceMap, interfaceComments),
                peers = peerMaps.map { (map, comments) -> buildPeer(map, comments) },
            )
        }

        internal fun buildInterface(m: Map<String, String>, comments: List<String>) =
            InterfaceSection(
                comments = comments,
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
                h1 = m["H1"],
                h2 = m["H2"],
                h3 = m["H3"],
                h4 = m["H4"],
                i1 = m["I1"],
                i2 = m["I2"],
                i3 = m["I3"],
                i4 = m["I4"],
                i5 = m["I5"],
                includedApplications = m.getList("IncludedApplications"),
                excludedApplications = m.getList("ExcludedApplications"),
            )

        private fun buildPeer(m: Map<String, String>, comments: List<String>) =
            PeerSection(
                publicKey = m["PublicKey"] ?: "",
                allowedIPs = m["AllowedIPs"],
                endpoint = m["Endpoint"],
                presharedKey = m["PresharedKey"],
                persistentKeepalive = m.getInt("PersistentKeepalive", "Peer"),
                comments = comments,
            )

        fun parseEndpoint(endpoint: String): Pair<String?, String?> {
            var host: String
            var portStr: String?
            if (endpoint.startsWith("[")) {
                val endBracket = endpoint.lastIndexOf("]")
                if (endBracket == -1 || !endpoint.substring(endBracket + 1).startsWith(":"))
                    return null to null
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

        internal fun hexToBase64(hex: String): String {
            if (hex.length != 64 || !hex.matches(Regex("[0-9a-fA-F]{64}"))) {
                throw ConfigParseException(ErrorType.INVALID_HEX_KEY, "key", hex)
            }
            val bytes = ByteArray(32)
            for (i in 0 until 32) {
                val chunk = hex.substring(i * 2, i * 2 + 2)
                bytes[i] = chunk.toInt(16).toByte()
            }
            return Base64.encode(bytes)
        }

        internal fun buildActivePeer(m: Map<String, String>) =
            ActivePeer(
                publicKey = m["PublicKey"] ?: "",
                allowedIPs = m["AllowedIPs"],
                endpoint = m["Endpoint"],
                presharedKey = m["PresharedKey"],
                persistentKeepalive = m.getInt("PersistentKeepalive", "Peer"),
                lastHandshakeSeconds = m.getLong("LastHandshakeSeconds", "Peer"),
                lastHandshakeNanos = m.getLong("LastHandshakeNanos", "Peer"),
                txBytes = m.getLong("TxBytes", "Peer"),
                rxBytes = m.getLong("RxBytes", "Peer"),
            )

        internal fun generatePublicKeyFromPrivate(privateBase64: String): String {
            val privateKey = Key.fromBase64(privateBase64)
            val publicKey = Key.generatePublicKey(privateKey)
            return publicKey.toBase64()
        }
    }
}

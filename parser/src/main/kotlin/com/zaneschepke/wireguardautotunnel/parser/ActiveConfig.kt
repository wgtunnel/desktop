package com.zaneschepke.wireguardautotunnel.parser

import com.zaneschepke.wireguardautotunnel.parser.util.ConfigFormatter
import kotlinx.serialization.Serializable

@Serializable
data class ActiveConfig(val interfaceSection: InterfaceSection, val peers: List<ActivePeer>) {

    // user readable active config in ini format
    fun asQuickString(): String =
        buildString {
                ConfigFormatter.appendInterfaceSection(this, interfaceSection)
                peers.forEach { ConfigFormatter.appendActivePeerSection(this, it) }
            }
            .trim()

    companion object {
        fun parseFromIpc(ipcString: String): ActiveConfig {
            val interfaceMap = mutableMapOf<String, String>()
            val peerMaps = mutableListOf<MutableMap<String, String>>()
            var currentPeerMap: MutableMap<String, String>? = null

            val normalized = ipcString.replace("\r\n", "\n").replace("\r", "\n").trim()
            normalized.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    return@forEach

                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    if (key in listOf("private_key", "public_key", "preshared_key")) {
                        value = value.replace(Regex("\\s+"), "")
                        if (value.length == 64 && value.matches(Regex("[0-9a-fA-F]{64}"))) {
                            value = Config.hexToBase64(value)
                        }
                    }

                    when (key) {
                        // Interface keys
                        "private_key" -> interfaceMap["PrivateKey"] = value
                        "listen_port" -> interfaceMap["ListenPort"] = value
                        "fwmark" -> interfaceMap["FwMark"] = value
                        "jc" -> interfaceMap["Jc"] = value
                        "jmin" -> interfaceMap["Jmin"] = value
                        "jmax" -> interfaceMap["Jmax"] = value
                        "s1" -> interfaceMap["S1"] = value
                        "s2" -> interfaceMap["S2"] = value
                        "s3" -> interfaceMap["S3"] = value
                        "s4" -> interfaceMap["S4"] = value
                        "h1" -> interfaceMap["H1"] = value
                        "h2" -> interfaceMap["H2"] = value
                        "h3" -> interfaceMap["H3"] = value
                        "h4" -> interfaceMap["H4"] = value
                        "i1" -> interfaceMap["I1"] = value
                        "i2" -> interfaceMap["I2"] = value
                        "i3" -> interfaceMap["I3"] = value
                        "i4" -> interfaceMap["I4"] = value
                        "i5" -> interfaceMap["I5"] = value

                        "public_key" -> {
                            currentPeerMap = mutableMapOf<String, String>().also { peerMaps += it }
                            currentPeerMap["PublicKey"] = value
                        }
                        "preshared_key" -> currentPeerMap?.put("PresharedKey", value)
                        "endpoint" -> currentPeerMap?.put("Endpoint", value)
                        "persistent_keepalive_interval" ->
                            currentPeerMap?.put("PersistentKeepalive", value)
                        "last_handshake_time_sec" ->
                            currentPeerMap?.put("LastHandshakeSeconds", value)
                        "last_handshake_time_nsec" ->
                            currentPeerMap?.put("LastHandshakeNanos", value)
                        "tx_bytes" -> currentPeerMap?.put("TxBytes", value)
                        "rx_bytes" -> currentPeerMap?.put("RxBytes", value)
                        "protocol_version" -> currentPeerMap?.put("ProtocolVersion", value)
                        "allowed_ip" -> {
                            val existing = currentPeerMap?.get("AllowedIPs") ?: ""
                            currentPeerMap?.put(
                                "AllowedIPs",
                                if (existing.isEmpty()) value else "$existing,$value",
                            )
                        }
                    }
                }
            }

            if (interfaceMap.isEmpty() && peerMaps.isEmpty()) {
                throw ConfigParseException(
                    ErrorType.IPC_PARSE_FAILED,
                    "No valid sections found in IPC string",
                )
            }

            return ActiveConfig(
                interfaceSection = Config.buildInterface(interfaceMap, emptyList()),
                peers = peerMaps.map { Config.buildActivePeer(it) },
            )
        }
    }
}

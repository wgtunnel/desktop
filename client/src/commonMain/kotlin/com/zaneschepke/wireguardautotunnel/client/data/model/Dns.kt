package com.zaneschepke.wireguardautotunnel.client.data.model

enum class DnsProtocol(val value: Int) {
    SYSTEM(0),
    DOH(1);

    companion object {
        fun fromValue(value: Int): com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol =
            _root_ide_package_.com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol.entries.find { it.value == value } ?: SYSTEM
    }
}

enum class DnsProvider(private val systemAddress: String, private val dohAddress: String) {
    CLOUDFLARE("1.1.1.1", "https://1.1.1.1/dns-query"),
    ADGUARD("94.140.14.14", "https://94.140.14.14/dns-query");

    fun asAddress(protocol: com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol): String {
        return when (protocol) {
            _root_ide_package_.com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol.SYSTEM -> systemAddress
            _root_ide_package_.com.zaneschepke.wireguardautotunnel.client.data.model.DnsProtocol.DOH -> dohAddress
        }
    }

    companion object {
        fun fromAddress(address: String): com.zaneschepke.wireguardautotunnel.client.data.model.DnsProvider {
            return entries.find { it.systemAddress == address || it.dohAddress == address }
                ?: CLOUDFLARE
        }
    }
}
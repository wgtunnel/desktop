package com.zaneschepke.wireguardautotunnel.client.domain.enums

import com.wgtunnel.backend.model.dns.ForeignDnsPolicy as CoreForeignDnsPolicy

enum class TransitDnsPolicy(val value: Int) {
    Redirect(0),
    Block(1),
    Allow(2);

    fun toCore(): CoreForeignDnsPolicy =
        when (this) {
            Redirect -> CoreForeignDnsPolicy.REDIRECT
            Block -> CoreForeignDnsPolicy.BLOCK
            Allow -> CoreForeignDnsPolicy.ALLOW
        }

    companion object {
        fun fromValue(value: Int): TransitDnsPolicy = entries.find { it.value == value } ?: Redirect

        fun fromName(name: String): TransitDnsPolicy =
            when (name.trim().uppercase()) {
                "BLOCK",
                "DROP" -> Block
                "ALLOW" -> Allow
                else -> Redirect
            }
    }
}

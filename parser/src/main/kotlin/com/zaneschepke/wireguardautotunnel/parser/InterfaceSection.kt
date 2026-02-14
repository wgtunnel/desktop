package com.zaneschepke.wireguardautotunnel.parser

import com.zaneschepke.wireguardautotunnel.parser.util.NetworkUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterfaceSection(
    @SerialName("PrivateKey") val privateKey: String,
    @SerialName("Address") val address: String? = null,
    @SerialName("ListenPort") val listenPort: Int? = null,
    @SerialName("DNS") val dns: String? = null,
    @SerialName("MTU") val mtu: Int? = null,
    // Linux
    @SerialName("FwMark") val fwMark: Int? = null,
    @SerialName("Table") val table: String? = null,
    @SerialName("SaveConfig") val saveConfig: Boolean? = null,
    // Desktop or Rooted Android
    @SerialName("PreUp") val preUp: String? = null,
    @SerialName("PostUp") val postUp: String? = null,
    @SerialName("PreDown") val preDown: String? = null,
    @SerialName("PostDown") val postDown: String? = null,
    // Android
    @SerialName("IncludedApplications") val includedApplications: List<String>? = null,
    @SerialName("ExcludedApplications") val excludedApplications: List<String>? = null,
    // Amnezia
    @SerialName("Jc") val jC: Int? = null,
    @SerialName("Jmin") val jMin: Int? = null,
    @SerialName("Jmax") val jMax: Int? = null,
    @SerialName("S1") val s1: Int? = null,
    @SerialName("S2") val s2: Int? = null,
    @SerialName("S3") val s3: Int? = null,
    @SerialName("S4") val s4: Int? = null,
    @SerialName("H1") val h1: String? = null,
    @SerialName("H2") val h2: String? = null,
    @SerialName("H3") val h3: String? = null,
    @SerialName("H4") val h4: String? = null,
    @SerialName("I1") val i1: String? = null,
    @SerialName("I2") val i2: String? = null,
    @SerialName("I3") val i3: String? = null,
    @SerialName("I4") val i4: String? = null,
    @SerialName("I5") val i5: String? = null,
    val comments: List<String> = emptyList(),
) {
    @Throws(ConfigParseException::class)
    fun validate() {
        if (privateKey.isBlank())
            throw ConfigParseException(ErrorType.MISSING_REQUIRED_FIELD, "Interface.PrivateKey")
        if (!NetworkUtils.isValidBase64(privateKey))
            throw ConfigParseException(
                ErrorType.INVALID_BASE64_KEY,
                "Interface.PrivateKey",
                privateKey,
            )

        listenPort?.let {
            if (it !in 0..65535)
                throw ConfigParseException(ErrorType.INVALID_PORT_RANGE, "Interface.ListenPort", it)
        }
        mtu?.let {
            if (it !in 576..9000)
                throw ConfigParseException(ErrorType.INVALID_MTU_RANGE, "Interface.MTU", it)
        }
        fwMark?.let {
            if (it < 0) throw ConfigParseException(ErrorType.INVALID_FWMARK, "Interface.FwMark", it)
        }

        jC?.let {
            if (it !in 4..12)
                throw ConfigParseException(ErrorType.INVALID_JC_RANGE, "Interface.Jc", it)
        }
        if (jMin != null && jMax != null) {
            if (jMin > jMax)
                throw ConfigParseException(ErrorType.INVALID_JMIN_JMAX_ORDER, "Interface.Jmin/Jmax")
            if (jMax >= (mtu ?: 1500))
                throw ConfigParseException(ErrorType.INVALID_JMAX_MTU, "Interface.Jmax", jMax)
        }

        listOf(s1, s2, s3, s4).forEachIndexed { i, s ->
            if (s != null && s < 0)
                throw ConfigParseException(
                    ErrorType.INVALID_PADDING_NEGATIVE,
                    "Interface.S${i + 1}",
                    s,
                )
        }

        listOf(h1, h2, h3, h4).forEachIndexed { i, h ->
            if (h != null && !NetworkUtils.isValidAmneziaHeader(h)) {
                throw ConfigParseException(
                    ErrorType.INVALID_HEADER_FORMAT,
                    "Interface.H${i + 1}",
                    h,
                )
            }
        }

        listOf(i1, i2, i3, i4, i5).forEachIndexed { i, sig ->
            if (sig != null && !NetworkUtils.isValidHexSignature(sig)) {
                throw ConfigParseException(
                    ErrorType.INVALID_SIGNATURE_FORMAT,
                    "Interface.I${i + 1}",
                    sig,
                )
            }
        }

        address
            ?.split(",")
            ?.map { it.trim() }
            ?.forEach {
                if (it.isNotBlank() && !NetworkUtils.isValidCidr(it))
                    throw ConfigParseException(ErrorType.INVALID_CIDR, "Interface.Address", it)
            }
        dns?.split(",")
            ?.map { it.trim() }
            ?.forEach {
                if (it.isNotBlank() && !NetworkUtils.isValidDnsEntry(it))
                    throw ConfigParseException(ErrorType.INVALID_DNS_ENTRY, "Interface.DNS", it)
            }
    }
}

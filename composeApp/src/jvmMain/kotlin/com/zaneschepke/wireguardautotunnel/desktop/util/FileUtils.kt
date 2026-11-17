package com.zaneschepke.wireguardautotunnel.desktop.util

import com.wgtunnel.parser.archive.ConfigArchive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object FileUtils {

    const val CONF_FILE_EXTENSION = "conf"
    const val ZIP_FILE_EXTENSION = "zip"

    fun readConfigsFromZip(bytes: ByteArray): Map<String, String> =
        ConfigArchive.read(ByteArrayInputStream(bytes)).associate { it.quickConfig to it.name }

    fun createZipArchive(entries: List<ConfigArchive.Entry>): ByteArray =
        ByteArrayOutputStream().also { ConfigArchive.write(it, entries) }.toByteArray()
}

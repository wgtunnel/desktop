package com.zaneschepke.wireguardautotunnel.desktop.util

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper.getDatabaseDir
import java.io.*
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    const val CONF_FILE_EXTENSION = "conf"
    const val ZIP_FILE_EXTENSION = "zip"

    fun readConfigsFromZip(bytes: ByteArray): Map<String, String> {
        val configs = mutableMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val baseName = entry.name.substringAfterLast("/").lowercase()

                // only process files that end in .conf and aren't hidden or dirs
                if (
                    !entry.isDirectory &&
                        baseName.endsWith(".$CONF_FILE_EXTENSION") &&
                        !baseName.startsWith(".")
                ) {
                    // extract name
                    val tunnelName = entry.name.substringAfterLast("/").substringBeforeLast(".")

                    val content = zip.bufferedReader().readText()
                    if (content.isNotBlank()) {
                        configs[content] = tunnelName
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return configs
    }

    fun createZipArchive(configs: Map<String, String>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(BufferedOutputStream(byteArrayOutputStream)).use { zos ->
            configs.forEach { (name, content) ->
                // make each end with .conf
                val entryName =
                    if (name.endsWith(".$CONF_FILE_EXTENSION")) name
                    else "$name.$CONF_FILE_EXTENSION"
                val entry = ZipEntry(entryName)

                zos.putNextEntry(entry)
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
            zos.finish()
        }
        return byteArrayOutputStream.toByteArray()
    }

    fun getConfigFile(): File {
        val configDir = getDatabaseDir()
        if (!configDir.exists()) configDir.mkdirs()
        return File(configDir, "config.properties")
    }

    fun loadAppConfig(): AppConfig {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            createDefaultConfigTemplate(configFile)
        }

        return try {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }
            AppConfig.fromProperties(props)
        } catch (_: Exception) {
            Logger.w { "Could not read config.properties, using defaults" }
            AppConfig()
        }
    }

    private fun createDefaultConfigTemplate(file: File) {
        file.writeText(
            """
            # WireGuard Auto Tunnel - Advanced Configuration
            # Edit this file and restart the app for changes to apply.
            #
            # rendering=hardware      (default, best performance, recommended)
            # rendering=software      (use only if you have issues rendering the app)
            #
            rendering=hardware
            """
                .trimIndent()
        )
    }
}

package com.zaneschepke.wireguardautotunnel.core.profile

import dev.nucleusframework.core.runtime.ExecutableRuntime

// `-Dapp.variant=` or `WGTUNNEL_VARIANT` env var for specifying variant
enum class AppVariant(val id: String) {
    DEBUG("debug"),
    BETA("beta"),
    RELEASE("release");

    val linuxFsName: String
        get() =
            when (this) {
                RELEASE -> "wgtunnel"
                else -> "wgtunnel-$id"
            }

    val desktopAppName: String
        get() =
            when (this) {
                RELEASE -> "WGTunnel"
                DEBUG -> "WGTunnel Debug"
                BETA -> "WGTunnel Beta"
            }

    val displaySuffix: String
        get() =
            when (this) {
                RELEASE -> ""
                DEBUG -> " Debug"
                BETA -> " Beta"
            }

    val ipcFolder: String
        get() =
            when (this) {
                RELEASE -> "wgtunnel"
                else -> "wgtunnel-$id"
            }

    val keyringService: String
        get() =
            when (this) {
                RELEASE -> "wg_tunnel"
                else -> "wg_tunnel_$id"
            }

    val ifacePrefix: String
        get() =
            when (this) {
                RELEASE -> "wgtun"
                DEBUG -> "wgtund"
                BETA -> "wgtunb"
            }

    val lockIdentifier: String
        get() =
            when (this) {
                RELEASE -> "wg_tunnel"
                else -> "wg_tunnel_$id"
            }

    /** Nucleus auto-update channel this variant */
    val updateChannel: String
        get() =
            when (this) {
                BETA -> "beta"
                else -> "latest"
            }

    companion object {
        const val ENV_NAME = "WGTUNNEL_VARIANT"
        const val PROP_NAME = "app.variant"
        const val IFACE_PREFIX_PROP = "wgtunnel.iface.prefix"

        val current: AppVariant by lazy { resolve() }

        fun resolve(): AppVariant {
            parse(System.getProperty(PROP_NAME))?.let {
                return it
            }
            parse(System.getenv(ENV_NAME))?.let {
                return it
            }
            if (System.getenv("WG_TUNNEL_SERVICE") == "1") return RELEASE
            if (isPackaged()) {
                val fsname = System.getProperty("app.fsname").orEmpty()
                if (fsname.contains("beta", ignoreCase = true)) return BETA
                return RELEASE
            }
            return DEBUG
        }

        fun parse(raw: String?): AppVariant? {
            val value = raw?.trim()?.lowercase().orEmpty()
            if (value.isEmpty()) return null
            return entries.find { it.id == value }
        }

        fun isPackaged(): Boolean {
            if (!System.getProperty("app.dir").isNullOrBlank()) return true
            if (!System.getProperty("jpackage.app-path").isNullOrBlank()) return true
            val executableType = System.getProperty("nucleus.executable.type").orEmpty().lowercase()
            if (
                executableType.isNotEmpty() &&
                    executableType !in setOf("dev", "development", "app-image")
            ) {
                return true
            }

            if (ExecutableRuntime.isGraalVmNativeImage) {
                if (!ExecutableRuntime.isDev()) return true
                return readMacOsExecutableTypeMarker()?.let {
                    it.isNotEmpty() && it !in setOf("dev", "development", "app-image")
                } ?: false
            }

            return false
        }

        private fun readMacOsExecutableTypeMarker(): String? {
            val exePath = ProcessHandle.current().info().command().orElse(null) ?: return null
            var dir: java.io.File? = java.io.File(exePath).parentFile
            while (dir != null) {
                if (dir.name == "MacOS" && dir.parentFile?.name == "Contents") {
                    val marker = dir.parentFile.resolve("Resources/.nucleus-executable-type")
                    if (!marker.isFile) return null
                    return marker.readLines().firstOrNull()?.trim()?.lowercase()
                }
                dir = dir.parentFile
            }
            return null
        }

        /**
         * To be called once from each process entrypoint before opening the DB, IPC, or native
         * backend so core can pick up the iface prefix.
         */
        fun applyProcessDefaults() {
            val variant = current
            System.setProperty(PROP_NAME, variant.id)
            System.setProperty(IFACE_PREFIX_PROP, variant.ifacePrefix)
        }
    }
}

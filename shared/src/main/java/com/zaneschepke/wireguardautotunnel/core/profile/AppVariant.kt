package com.zaneschepke.wireguardautotunnel.core.profile

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
                RELEASE -> ".wgtunnel"
                else -> ".wgtunnel-$id"
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

            if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
                val exePath = ProcessHandle.current().info().command().orElse(null) ?: return false
                val exeDir = java.nio.file.Path.of(exePath).parent ?: return false
                // main binary sits at <root>/wgtunnel while daemon sits one level down at
                // <root>/bin/daemon
                val candidates =
                    listOf(
                        exeDir.resolve(".nucleus-executable-type"),
                        exeDir.parent?.resolve(".nucleus-executable-type"),
                    )
                val marker =
                    candidates.firstNotNullOfOrNull { path ->
                        path?.takeIf { java.nio.file.Files.isRegularFile(it) }
                    } ?: return false
                val packageType =
                    java.nio.file.Files.readAllLines(marker).firstOrNull()?.lowercase().orEmpty()
                return packageType.isNotEmpty() &&
                    packageType !in setOf("dev", "development", "app-image")
            }

            return false
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

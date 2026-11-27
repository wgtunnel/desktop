import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.DmgFormat
import dev.nucleusframework.desktop.application.dsl.GarbageCollector
import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.ReleaseChannel
import dev.nucleusframework.desktop.application.dsl.ReleaseType
import dev.nucleusframework.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
    alias(libs.plugins.nucleus)
    alias(libs.plugins.buildconfig)
}

group = "com.zaneschepke.wireguardautotunnel"

val catalogVersion = libs.versions.app.get()
val packagingVariant =
    ((findProperty("app.variant") as String?) ?: System.getenv("WGTUNNEL_VARIANT") ?: "release")
        .trim()
        .lowercase()
val isBetaPackaging = packagingVariant == "beta"

val packageSemver = (findProperty("app.packageVersion") as String?) ?: catalogVersion
val appVersion = (findProperty("app.version") as String?) ?: catalogVersion
val appFsName = if (isBetaPackaging) "wgtunnel-beta" else "wgtunnel"
val appDisplayName = if (isBetaPackaging) "WG Tunnel Beta" else "WG Tunnel"

// Shown by macOS in the Location prompt. The app reads the Wi-Fi name only.
val macWifiLocationUsage =
    "WG Tunnel uses the Wi-Fi network name to match trusted networks for auto-tunnel. It does not track your location."

// The helper uses the container bundle id.
fun macWifiAgentInfoPlist(executable: String): String {
    fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>CFBundleIdentifier</key>
            <string>${xml("com.wgtunnel.$appFsName")}</string>
            <key>CFBundleName</key>
            <string>${xml(appDisplayName)}</string>
            <key>CFBundleDisplayName</key>
            <string>${xml(appDisplayName)}</string>
            <key>CFBundleDevelopmentRegion</key>
            <string>en</string>
            <key>CFBundleIconFile</key>
            <string>AppIcon</string>
            <key>LSHasLocalizedDisplayName</key>
            <true/>
            <key>CFBundleExecutable</key>
            <string>${xml(executable)}</string>
            <key>CFBundlePackageType</key>
            <string>APPL</string>
            <key>CFBundleInfoDictionaryVersion</key>
            <string>6.0</string>
            <key>CFBundleShortVersionString</key>
            <string>${xml(appVersion)}</string>
            <key>CFBundleVersion</key>
            <string>${xml(appVersion)}</string>
            <key>LSMinimumSystemVersion</key>
            <string>13.0</string>
            <key>LSUIElement</key>
            <true/>
            <key>NSLocationUsageDescription</key>
            <string>${xml(macWifiLocationUsage)}</string>
            <key>NSLocationWhenInUseUsageDescription</key>
            <string>${xml(macWifiLocationUsage)}</string>
            <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
            <string>${xml(macWifiLocationUsage)}</string>
        </dict>
        </plist>
        """
        .trimIndent() + "\n"
}

// Location Services falls back to the .app folder name, and ignores an unlocalized
// CFBundleDisplayName. The nested bundle is named for the app and carries this strings file.
fun macWifiAgentDisplayStrings(): String {
    fun strings(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
    val name = strings(appDisplayName)
    return """
        CFBundleName = "$name";
        CFBundleDisplayName = "$name";
        """
        .trimIndent() + "\n"
}

version = appVersion

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":client"))
            implementation(libs.wgtunnel.parser)
            implementation(libs.human.readable)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.bundles.navigation3)
            implementation(libs.bundles.compose.icons)

            implementation((libs.kotlinx.serialization))

            // Licenses
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.core)
            implementation(libs.aboutlibraries.compose.m3)

            implementation(libs.bundles.jetbrains.lifecycle)

            // Files
            implementation(libs.kmp.io)
            implementation(libs.bundles.filekit)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.bundles.orbit.mvi)

            // Logging
            implementation(libs.kermit)

            // UI
            implementation(libs.sonner)
            implementation(libs.material.kolor)
            implementation(libs.colorpicker.compose)
            implementation(libs.localina)
            implementation(libs.reorderable)
            implementation(libs.compose.native.tray)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.wgtunnel.backend)

            implementation(libs.bundles.nucleus)
            implementation(libs.nucleus.graalvm.runtime)
            implementation(libs.nucleus.updater.runtime)
            implementation(libs.nucleus.autolaunch)
            implementation(libs.nucleus.service.management.macos)
            implementation(project(":daemon"))
        }
    }
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

// BCP-47 tags that have a translation
val languagesProvider = providers.provider {
    fileTree("src/jvmMain/composeResources") { include("values-*/strings.xml") }
        .map { it.parentFile.name.removePrefix("values-").replace("-r", "-") }
        .distinct()
        .sorted() + "en"
}

buildConfig {
    buildConfigField("APP_VERSION", provider { "${project.version}" })
    buildConfigField("LANGUAGES", languagesProvider.map { it.joinToString(",") })
    // GraalVM drops -Dapp.variant at runtime, so the app reads its variant from here.
    buildConfigField("VARIANT", packagingVariant)
}

// Crowdin's Android format exporter always backslash escapes apostrophes/quotes, so we clean these
// up during build.
// https://community.crowdin.com/t/compose-multiplatform-xml-should-not-escape-android-special-characters/14364).
val unescapeTranslatedStrings =
    tasks.register("unescapeTranslatedStrings") {
        description = "Strips Android-style backslash escaping from composeResources strings.xml"
        val stringsFiles =
            fileTree("src/jvmMain/composeResources") { include("values*/strings.xml") }
        inputs.files(stringsFiles)
        outputs.upToDateWhen { false }
        doLast {
            stringsFiles.forEach { file ->
                val original = file.readText()
                val fixed = original.replace("\\'", "'").replace("\\\"", "\"")
                if (fixed != original) file.writeText(fixed)
            }
        }
    }

tasks.named("convertXmlValueResourcesForJvmMain") { dependsOn(unescapeTranslatedStrings) }

val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
val isMacOS = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
val macDaemonBundleIdValue = "com.wgtunnel.$appFsName.daemon"
val macWifiAgentBundleIdValue = "com.wgtunnel.$appFsName.wifi"
val packagedJvmArgs =
    listOf(
        "-Dapp.variant=$packagingVariant",
        "--enable-native-access=ALL-UNNAMED",
        "-Xms32m",
        "-Xmx128m",
        "-XX:MaxMetaspaceSize=128m",
        "-XX:ReservedCodeCacheSize=64m",
    )

val packagingAppFsName = appFsName
val packagingAppDisplayName = appDisplayName
val stagePackagingSidecars =
    tasks.register<StagePackagingSidecarsTask>("stagePackagingSidecars") {
        description = "State packaging sidecars"
        appFsName.set(packagingAppFsName)
        appDisplayName.set(packagingAppDisplayName)
        windows.set(isWindows)
        macOS.set(isMacOS)
        macDaemonBundleId.set(macDaemonBundleIdValue)
        macDaemonPlist.set(rootProject.file("packaging/macos/com.wgtunnel.daemon.plist"))
        macWifiAgentBundleId.set(macWifiAgentBundleIdValue)
        macWifiAgentPlist.set(rootProject.file("packaging/macos/com.wgtunnel.wifi-agent.plist"))
        appVariant.set(packagingVariant)
        linuxService.set(rootProject.file("packaging/linux/wgtunnel-daemon.service"))
        linuxEarlyBootService.set(rootProject.file("packaging/linux/wgtunnel-early-boot.service"))
        linuxInstallScript.set(rootProject.file("packaging/linux/tar-install.sh"))
        linuxUninstallScript.set(rootProject.file("packaging/linux/tar-uninstall.sh"))
        linuxAfterInstallScript.set(rootProject.file("packaging/linux/after-install.sh"))
        linuxBeforeInstallScript.set(rootProject.file("packaging/linux/before-install.sh"))
        linuxBeforeRemoveScript.set(rootProject.file("packaging/linux/before-remove.sh"))
        outputDir.set(layout.buildDirectory.dir("packaging-sidecars"))
        hooksOutputDir.set(layout.buildDirectory.dir("packaging-hooks"))
        if (isWindows) {
            dependsOn(":daemon:buildWinSW")
            windowsServiceXml.set(rootProject.file("packaging/windows/service-wrapper.xml"))
            val windowsArch = (findProperty("windows.arch") as String?) ?: "x64"
            val windowsPlatform = if (windowsArch == "arm64") "ARM64" else windowsArch
            winSwPublishDir.set(
                project(":daemon")
                    .layout
                    .projectDirectory
                    .dir(
                        "winsw/artifacts/bin/WinSW/$windowsPlatform/Release/net7.0-windows/win-$windowsArch/publish",
                    ),
            )
        }
    }

// Combines stagePackagingSidecars "common" output with the compiled daemon binary
// (nested under common/bin/) into one tree, fed to nativeDistributions.appResourcesRootDir.
val stageAppResources =
    tasks.register<Sync>("stageAppResources") {
        description =
            "Combine packaging sidecars and the compiled daemon binary for appResourcesRootDir"
        into(layout.buildDirectory.dir("app-resources-root"))
        from(stagePackagingSidecars.map { it.outputDir })
        into("common/bin") {
            from(
                fileTree(
                    project(":daemon")
                        .layout
                        .buildDirectory
                        .dir("compose/tmp/main/graalvm/nativeCompile"),
                ) {
                    include("wgtunnel-daemon", "wgtunnel-daemon.exe")
                    builtBy(":daemon:nativeImageCompile")
                },
            )
        }
        // The Wi-Fi helper for macOS
        if (isMacOS) {
            into("common/bin") {
                from(
                    fileTree(
                        project(":wifi-agent")
                            .layout
                            .buildDirectory
                            .dir("compose/tmp/main/graalvm/nativeCompile"),
                    ) {
                        include("wgtunnel-wifi")
                        builtBy(":wifi-agent:nativeImageCompile")
                    },
                ) {
                    rename { "$packagingAppFsName-wifi" }
                }
            }
        }
    }

nucleus.application {
    mainClass = "com.zaneschepke.wireguardautotunnel.desktop.MainKt"
    garbageCollector = GarbageCollector.SERIAL
    jvmArgs(*packagedJvmArgs.toTypedArray())

    graalvm {
        isEnabled = true
        imageName = appFsName
        optimization = NativeImageOptimization.SIZE
        detectOrphanProjectClasses = true
        maxHeapSize = "96m"
        buildArgs.add("-Dapp.variant=$packagingVariant")
        buildArgs.addAll(GraalvmNativeArgs.gui(System.getProperty("os.name").orEmpty()))
        // Match nativeDistributions version
        macOS { minimumSystemVersion = "13.0" }
    }

    additionalLaunchers {
        create("wgtunnel-daemon") {
            mainClass = "com.zaneschepke.wireguardautotunnel.daemon.MainKt"
            winConsole = false
            jvmArgs(
                "-Dapp.variant=$packagingVariant",
                "--enable-native-access=ALL-UNNAMED",
                "-Xms8m",
                "-Xmx48m",
                "-XX:ReservedCodeCacheSize=24m",
                "-Xss256k",
                "-Dkotlinx.coroutines.io.parallelism=8",
            )
        }
    }

    nativeDistributions {
        targetFormats(
            TargetFormat.Nsis,
            TargetFormat.Zip,
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.Pacman,
            TargetFormat.Tar,
            TargetFormat.Dmg,
        )
        appName = appDisplayName
        packageName = appFsName
        packageVersion = packageSemver
        vendor = "WG Tunnel"
        description = "WG Tunnel: An advanced, open-source client for WireGuard and AmneziaWG."
        homepage = "https://wgtunnel.com"
        copyright = "MIT"

        compressionLevel = CompressionLevel.Maximum

        // Temurin 25 has no jmods/ (JEP 493). includeAllModules would pull in jdk.jlink,
        // which that JDK cannot put in a custom runtime.
        modules(
            "java.xml",
            "java.prefs",
            "jdk.crypto.cryptoki",
            "jdk.charsets",
            "jdk.zipfs",
        )
        cleanupNativeLibs = true

        // appResourcesRootDir's common/ contents are copied next to the native executable
        appResourcesRootDir.set(layout.dir(stageAppResources.map { it.destinationDir }))

        publish {
            github {
                enabled = true
                owner = "wgtunnel"
                repo = "desktop"
                channel = if (isBetaPackaging) ReleaseChannel.Beta else ReleaseChannel.Latest
                releaseType = if (isBetaPackaging) ReleaseType.Prerelease else ReleaseType.Release
            }
        }

        linux {
            packageName = appFsName
            appCategory = "Network"
            debMaintainer = "WG Tunnel <support@wgtunnel.com>"
            // Leaving these empty makes electron-builder fall back to its own built-in default
            // dependency list
            pacmanDepends =
                listOf("gtk3", "gcc-libs", "freetype2", "fontconfig", "xdg-desktop-portal-gtk")
            rpmRequires =
                listOf(
                    "gtk3",
                    "glibc",
                    "libstdc++",
                    "freetype",
                    "fontconfig",
                    "(xdg-desktop-portal-gtk or xdg-desktop-portal-kde or xdg-desktop-portal-gnome)",
                )
            debDepends =
                listOf(
                    "libgtk-3-0",
                    "libc6",
                    "libstdc++6",
                    "libfreetype6",
                    "libfontconfig1",
                    "xdg-desktop-portal-gtk | xdg-desktop-portal-kde | xdg-desktop-portal-gnome",
                )
            iconFile.set(rootProject.file("packaging/linux/icon.png"))
            afterInstall.set(rootProject.file("packaging/linux/after-install.sh"))
            afterRemove.set(rootProject.file("packaging/linux/after-remove.sh"))
            // electron-builder doesn't substitute template variables in before hooks, so these
            // point at stagePackagingSidecars's pre-substituted copies instead of the raw
            // templates.
            beforeInstall.set(
                layout.file(
                    stagePackagingSidecars.map {
                        it.hooksOutputDir.get().asFile.resolve("before-install.sh")
                    },
                ),
            )
            beforeRemove.set(
                layout.file(
                    stagePackagingSidecars.map {
                        it.hooksOutputDir.get().asFile.resolve("before-remove.sh")
                    },
                ),
            )
            // Pacman only runs afterUpgrade/beforeUpgrade (never afterInstall/beforeInstall) when
            // replacing an already-installed package, so the same registration/stop logic needs to
            // be reachable from both
            afterUpgrade.set(
                layout.file(
                    stagePackagingSidecars.map {
                        it.hooksOutputDir.get().asFile.resolve("after-upgrade.sh")
                    },
                ),
            )
            beforeUpgrade.set(
                layout.file(
                    stagePackagingSidecars.map {
                        it.hooksOutputDir.get().asFile.resolve("before-install.sh")
                    },
                ),
            )
            appImage.desktopEntries =
                mapOf(
                    "Name" to appDisplayName,
                    "Categories" to "Network;Security;Settings;Utility;",
                )
        }

        windows {
            packageName = appFsName
            iconFile.set(rootProject.file("packaging/windows/icon.ico"))
            nsis {
                oneClick = false
                allowElevation = true
                perMachine = true
                menuCategory = appDisplayName
                shortcutName = appDisplayName
                includeScript.set(rootProject.file("packaging/windows/service.nsh"))
                deleteAppDataOnUninstall = true
                installerHeader.set(rootProject.file("packaging/windows/header.bmp"))
                installerSidebar.set(rootProject.file("packaging/windows/sidebar.bmp"))
                multiLanguageInstaller = true
                installerLanguages =
                    listOf(
                        "en_US",
                        "ru_RU",
                        "de_DE",
                        "nl_NL",
                        "fr_FR",
                    )
                license.set(rootProject.file("LICENSE"))
            }
            signing {
                val azureTenant = System.getenv("AZURE_TENANT_ID")
                if (!azureTenant.isNullOrBlank()) {
                    enabled = true
                    azureTenantId = azureTenant
                    azureEndpoint =
                        System.getenv("AZURE_CODE_SIGNING_ENDPOINT")
                            ?: "https://eus.codesigning.azure.net"
                    azureCertificateProfileName =
                        System.getenv("AZURE_CODE_SIGNING_CERTIFICATE_PROFILE") ?: "WG-Tunnel"
                    azureCodeSigningAccountName =
                        System.getenv("AZURE_CODE_SIGNING_ACCOUNT") ?: "WG-Tunnel"
                    publisherName = System.getenv("AZURE_CODE_SIGNING_PUBLISHER") ?: vendor
                }
            }
        }

        macOS {
            packageName = appFsName
            // Every release has shipped "WG Tunnel.app". We pin it so the path installs live at
            // (and the SMAppService registrations behind it) never change.
            bundleName = appDisplayName
            bundleID = "com.wgtunnel.$appFsName"
            dockName = appDisplayName
            appCategory = "public.app-category.utilities"
            // SMAppService's .daemon type (used to register the privileged
            // LaunchDaemon) requires macOS 13.0 (Ventura)+.
            minimumSystemVersion = "13.0"
            iconFile.set(rootProject.file("packaging/macos/icon.icns"))
            // core's native WireGuard backend is JNI-loaded at runtime and signed
            // independently of our Developer ID
            entitlementsFile.set(rootProject.file("packaging/macos/entitlements.plist"))
            // Location usage strings belong only on the Wi-Fi helper. Putting them
            // on this app makes System Settings list a second WG Tunnel row.

            dmg { format = DmgFormat.ULFO }

            signing {
                val identity = System.getenv("APPLE_SIGNING_IDENTITY")
                if (!identity.isNullOrBlank()) {
                    sign.set(true)
                    this.identity.set(identity)
                    System.getenv("APPLE_SIGNING_KEYCHAIN")?.let { keychain.set(it) }
                }
            }

            notarization {
                val apiKeyPath = System.getenv("APPLE_NOTARIZATION_API_KEY_PATH")
                val apiKeyId = System.getenv("APPLE_NOTARIZATION_API_KEY_ID")
                val apiIssuer = System.getenv("APPLE_NOTARIZATION_API_ISSUER")
                val keychainProfileEnv = System.getenv("APPLE_NOTARIZATION_KEYCHAIN_PROFILE")
                val teamId = System.getenv("APPLE_TEAM_ID")
                when {
                    !apiKeyPath.isNullOrBlank() &&
                        !apiKeyId.isNullOrBlank() &&
                        !apiIssuer.isNullOrBlank() -> {
                        apiKey.set(apiKeyPath)
                        this.apiKeyId.set(apiKeyId)
                        this.apiIssuer.set(apiIssuer)
                    }
                    // Local dev fallback
                    !keychainProfileEnv.isNullOrBlank() -> keychainProfile.set(keychainProfileEnv)
                    // Local dev fallback
                    !teamId.isNullOrBlank() -> {
                        this.teamID.set(teamId)
                        System.getenv("APPLE_ID")?.let { appleID.set(it) }
                        System.getenv("APPLE_APP_SPECIFIC_PASSWORD")?.let { password.set(it) }
                    }
                    else -> Unit
                }
            }
        }
    }
}

val graalvmLaunchersDir = layout.buildDirectory.dir("compose/binaries/main/graalvm-app/$appFsName")
val graalvmNativeCompileDirs =
    listOf(project(":daemon").layout.buildDirectory.dir("compose/tmp/main/graalvm/nativeCompile"))
val graalvmLauncherNames = listOf("wgtunnel-daemon", "wgtunnel-daemon.exe")
val copyGraalvmSidecarDepends =
    listOf(
        ":daemon:nativeImageCompile",
        "copyGraalvmBinaryToOutput",
    )

val windowsNativeArch =
    when (
        (findProperty("windows.arch") as String?)?.lowercase()
            ?: System.getProperty("os.arch").orEmpty()
    ) {
        "arm64",
        "aarch64" -> "aarch64"
        else -> "x64"
    }

val wintunDllFile = rootProject.file("packaging/windows/wintun/win32-$windowsNativeArch/wintun.dll")

// Service unit / WinSW / install-script sidecars reach the app image via
// stageAppResources -> nativeDistributions.appResourcesRootDir
val graalvmSidecarTaskNames = mutableListOf<String>()

// Windows-only: WinSW needs the daemon exe and wintun.dll sitting next to the GUI binary at the
// app root, not under bin/ so this copy stays. On Linux/other platforms the daemon binary already
// reaches bin/ via
// stageAppResources -> appResourcesRootDir, so no equivalent task is needed there.
if (isWindows) {
    tasks.register<Copy>("copyGraalvmExtraLaunchersToRoot") {
        group = "nucleus"
        description = "Copy native daemon and wintun.dll next to the GraalVM GUI binary (WinSW)."
        dependsOn(copyGraalvmSidecarDepends)
        doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
        graalvmNativeCompileDirs.forEach { dir ->
            from(dir) { include(*graalvmLauncherNames.toTypedArray()) }
        }
        from(wintunDllFile)
        into(graalvmLaunchersDir)
    }
    graalvmSidecarTaskNames += "copyGraalvmExtraLaunchersToRoot"

    // electron-builder runs with --prepackaged, which skipssigning of the app's executables,
    // so it signed only the installer and uninstaller.  Sign our unsigned executables here.
    // Files already signed are left as they are.
    val signingTenant = System.getenv("AZURE_TENANT_ID")
    tasks.register<Exec>("signWindowsExecutables") {
        group = "nucleus"
        description = "Sign the app's unsigned executables with Azure Trusted Signing."
        dependsOn("copyGraalvmExtraLaunchersToRoot")
        onlyIf { !signingTenant.isNullOrBlank() }
        doNotTrackState("Signs files in place in the shared graalvm-app dir")
        environment("WGT_SIGN_DIR", graalvmLaunchersDir.get().asFile.absolutePath)
        environment(
            "WGT_SIGN_ENDPOINT",
            System.getenv("AZURE_CODE_SIGNING_ENDPOINT") ?: "https://eus.codesigning.azure.net",
        )
        environment(
            "WGT_SIGN_PROFILE",
            System.getenv("AZURE_CODE_SIGNING_CERTIFICATE_PROFILE") ?: "WG-Tunnel",
        )
        environment("WGT_SIGN_ACCOUNT", System.getenv("AZURE_CODE_SIGNING_ACCOUNT") ?: "WG-Tunnel")
        commandLine(
            "powershell",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}files = @(Get-ChildItem -LiteralPath ${'$'}env:WGT_SIGN_DIR -Recurse -File -Filter *.exe |
                Where-Object { (Get-AuthenticodeSignature -LiteralPath ${'$'}_.FullName).Status -eq 'NotSigned' } |
                ForEach-Object FullName)
            if (${'$'}files.Count -eq 0) { Write-Host 'No unsigned executables'; exit 0 }
            try { Install-PackageProvider -Name NuGet -MinimumVersion 2.8.5.201 -Force -Scope CurrentUser | Out-Null } catch {}
            if (-not (Get-Module -ListAvailable -Name TrustedSigning)) {
                Install-Module -Name TrustedSigning -MinimumVersion 0.5.0 -Force -Repository PSGallery -Scope CurrentUser
            }
            ${'$'}sign = @{
                Endpoint = ${'$'}env:WGT_SIGN_ENDPOINT
                CertificateProfileName = ${'$'}env:WGT_SIGN_PROFILE
                CodeSigningAccountName = ${'$'}env:WGT_SIGN_ACCOUNT
                TimestampRfc3161 = 'http://timestamp.acs.microsoft.com'
                TimestampDigest = 'SHA256'
                FileDigest = 'SHA256'
                Files = ${'$'}files -join ','
            }
            Invoke-TrustedSigning @sign
            ${'$'}bad = @(${'$'}files | Where-Object { (Get-AuthenticodeSignature -LiteralPath ${'$'}_).Status -ne 'Valid' })
            if (${'$'}bad.Count -gt 0) { throw "Not validly signed: ${'$'}(${'$'}bad -join ', ')" }
            Write-Host "Signed: ${'$'}(${'$'}files -join ', ')"
            """
                .trimIndent(),
        )
    }
    graalvmSidecarTaskNames += "signWindowsExecutables"
}

// SMAppService's .daemon type requires the LaunchDaemon plist embedded at
// Contents/Library/LaunchDaemons/ inside the .app bundle
if (isMacOS) {
    val stageMacDaemonPlist =
        tasks.register<Copy>("stageMacDaemonPlist") {
            group = "nucleus"
            description =
                "Embed the daemon LaunchDaemon plist into the built .app bundle for SMAppService."
            // THe copy copyGraalvmSidecarDepends equivalent for macOS
            dependsOn("copyGraalvmBinaryToApp", stagePackagingSidecars)
            doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
            from(
                stagePackagingSidecars.map {
                    it.outputDir.get().asFile.resolve(StagePackagingSidecarsTask.MAC_LAUNCHD_DIR)
                },
            ) {
                include("*-daemon.plist")
            }
            into(macBundleContents("Library/LaunchDaemons"))
        }
    val stageMacWifiAgentPlist =
        tasks.register<Copy>("stageMacWifiAgentPlist") {
            group = "nucleus"
            description =
                "Embed the Wi-Fi LaunchAgent plist into the built .app bundle for SMAppService."
            dependsOn("copyGraalvmBinaryToApp", stagePackagingSidecars)
            doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
            from(
                stagePackagingSidecars.map {
                    it.outputDir.get().asFile.resolve(StagePackagingSidecarsTask.MAC_LAUNCHD_DIR)
                },
            ) {
                include("*-wifi.plist")
            }
            into(macBundleContents("Library/LaunchAgents"))
        }
    graalvmSidecarTaskNames += "stageMacDaemonPlist"
    graalvmSidecarTaskNames += "stageMacWifiAgentPlist"

    // A bare executable has no NSBundle, so CoreWLAN keeps returning a nil name.
    // The helper is its own app and keeps the container bundle id, so it is the
    // same Location Services client as WG Tunnel. The nested bundle is named
    // "WG Tunnel.app" (or "WG Tunnel Beta.app"): Location Services shows that
    // folder name when it does not use CFBundleDisplayName.
    val macWifiAgentAppDir = "$appDisplayName.app"
    val stageMacWifiAgentBundle =
        tasks.register("stageMacWifiAgentBundle") {
            group = "nucleus"
            description =
                "Wrap the Wi-Fi helper in a nested app named for WG Tunnel, with its icon."
            dependsOn("copyGraalvmAppResources")
            doNotTrackState("Moves the helper binary inside the .app after resources are copied")
            doLast {
                val executable = "$appFsName-wifi"
                val src = macBundleContents("MacOS/bin/$executable").get()
                if (!src.isFile) {
                    error(
                        "stageMacWifiAgentBundle: Wi-Fi helper binary missing at ${src.absolutePath}",
                    )
                }
                macBundleContents("Resources/wifi-agent.app").get().deleteRecursively()
                val nestedRoot = macBundleContents("Resources/$macWifiAgentAppDir").get()
                val nestedMacOS = nestedRoot.resolve("Contents/MacOS")
                nestedMacOS.mkdirs()
                val dest = nestedMacOS.resolve(executable)
                src.copyTo(dest, overwrite = true)
                dest.setExecutable(true, false)
                if (src.absolutePath != dest.absolutePath) src.delete()
                nestedRoot
                    .resolve("Contents/Info.plist")
                    .writeText(macWifiAgentInfoPlist(executable))
                val resources = nestedRoot.resolve("Contents/Resources")
                resources.mkdirs()
                rootProject
                    .file("packaging/macos/icon.icns")
                    .copyTo(resources.resolve("AppIcon.icns"), overwrite = true)
                val stringsDir = resources.resolve("en.lproj")
                stringsDir.mkdirs()
                stringsDir.resolve("InfoPlist.strings").writeText(macWifiAgentDisplayStrings())
            }
        }
    graalvmSidecarTaskNames += "stageMacWifiAgentBundle"

    // resignApp Developer ID signs executables under Contents/MacOS, Frameworks,
    // and runtime, then seals the outer app without --deep. The helper lives
    // under Resources, so that pass never reaches it and notarization rejects
    // the ad-hoc signature left by codesignGraalvmBundle.
    val signMacWifiAgentBundle =
        tasks.register("signMacWifiAgentBundle") {
            group = "nucleus"
            description =
                "Developer ID sign the nested Wi-Fi helper with a timestamp and hardened runtime."
            dependsOn("codesignGraalvmBundle")
            doNotTrackState("Replaces the ad-hoc signature on the nested helper app")
            doLast {
                val identity = System.getenv("APPLE_SIGNING_IDENTITY")
                if (identity.isNullOrBlank()) return@doLast
                val nestedApp = macBundleContents("Resources/$macWifiAgentAppDir").get()
                if (!nestedApp.isDirectory) {
                    error(
                        "signMacWifiAgentBundle: nested helper app missing at ${nestedApp.absolutePath}",
                    )
                }
                val command = buildList {
                    add("/usr/bin/codesign")
                    add("--force")
                    add("--timestamp")
                    add("--options")
                    add("runtime")
                    add("--sign")
                    add(identity)
                    System.getenv("APPLE_SIGNING_KEYCHAIN")
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            add("--keychain")
                            add(it)
                        }
                    add("--entitlements")
                    add(rootProject.file("packaging/macos/entitlements.plist").absolutePath)
                    add(nestedApp.absolutePath)
                }
                val proc = ProcessBuilder(command).redirectErrorStream(true).start()
                val output = proc.inputStream.bufferedReader().readText()
                val code = proc.waitFor()
                if (code != 0) {
                    error("signMacWifiAgentBundle failed ($code): $output")
                }
            }
        }
    graalvmSidecarTaskNames += "signMacWifiAgentBundle"

    // The helper plist carries the Location usage strings. Strip them from the
    // container if a generator added any, so the GUI is not a second Location client.
    val patchMacWifiLocationPlist =
        tasks.register("patchMacWifiLocationPlist") {
            group = "nucleus"
            description = "Remove Wi-Fi Location usage keys from the main .app Info.plist."
            dependsOn("copyGraalvmInfoPlist")
            doNotTrackState("Mutates copyGraalvmInfoPlist's declared output in place")
            doLast {
                val plist = macBundleContents("Info.plist").get()
                val text = plist.readText()
                if (!text.contains("NSLocation")) return@doLast
                val stripped =
                    text.replace(
                        Regex(
                            """[ \t]*<key>NSLocation(?:WhenInUse|AlwaysAndWhenInUse)?UsageDescription</key>\s*<string>[^<]*</string>\s*""",
                        ),
                        "",
                    )
                if (stripped != text) plist.writeText(stripped)
            }
        }
    graalvmSidecarTaskNames += "patchMacWifiLocationPlist"
}

fun macBundleContents(child: String) = provider {
    val binariesRoot = layout.buildDirectory.dir("compose/binaries").get().asFile
    val appBundle =
        binariesRoot.walkTopDown().maxDepth(5).firstOrNull { dir ->
            dir.isDirectory &&
                dir.name.endsWith(".app") &&
                generateSequence(dir.parentFile) { it.parentFile }.none { it.name.endsWith(".app") }
        }
            ?: run {
                val listing =
                    if (binariesRoot.isDirectory) {
                        binariesRoot.walkTopDown().maxDepth(5).joinToString("\n") {
                            "  ${it.relativeTo(binariesRoot)}"
                        }
                    } else {
                        "<does not exist>"
                    }
                error(
                    "could not locate any .app bundle under $binariesRoot\nContents (depth 5):\n$listing",
                )
            }
    appBundle.resolve("Contents/$child")
}

tasks.withType<Copy>().configureEach {
    if (name.startsWith("copyGraalvm")) {
        doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
    }
}

afterEvaluate {
    tasks
        .matching { it.name.startsWith("packageGraalvm") }
        .configureEach { dependsOn(graalvmSidecarTaskNames) }

    // Signing comes after everything that writes into the graalvm-app dir, or a later copy
    // would replace a signed file with an unsigned one.
    if (isWindows) {
        tasks.named("signWindowsExecutables") {
            dependsOn(tasks.matching { it.name.startsWith("copyGraalvm") })
        }
    }

    // codesignGraalvmBundle currently runs before these, so its ad-hoc signature's seal
    // doesn't cover the plists, the nested helper app, or the Info.plist keys they add
    // afterward. Force these to run first instead, matching the existing
    // codesignGraalvmDylibs pattern.
    if (isMacOS) {
        tasks.named("codesignGraalvmBundle") {
            dependsOn(
                "stageMacDaemonPlist",
                "stageMacWifiAgentPlist",
                "stageMacWifiAgentBundle",
                "patchMacWifiLocationPlist",
            )
        }
    }
}

tasks.named("copyNonXmlValueResourcesForJvmMain") { dependsOn("exportLibraryDefinitions") }

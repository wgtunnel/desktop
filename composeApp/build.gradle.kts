import dev.nucleusframework.desktop.application.dsl.CompressionLevel
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
            implementation(project(":daemon"))
        }
    }
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
}

buildConfig { buildConfigField("APP_VERSION", provider { "${project.version}" }) }

val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
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
        linuxService.set(rootProject.file("packaging/linux/wgtunnel-daemon.service"))
        outputDir.set(layout.buildDirectory.dir("packaging-sidecars"))
        if (isWindows) {
            dependsOn(":daemon:buildWinSW")
            windowsServiceXml.set(rootProject.file("packaging/windows/service-wrapper.xml"))
            winSwPublishDir.set(
                project(":daemon")
                    .layout
                    .projectDirectory
                    .dir("winsw/artifacts/bin/WinSW/x64/Release/net7.0-windows/win-x64/publish")
            )
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
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.Pacman,
            TargetFormat.Tar,
        )
        appName = appDisplayName
        packageName = appFsName
        packageVersion = packageSemver
        vendor = "WG Tunnel"
        description =
            "WG Tunnel: WireGuard and AmneziaWG VPN client with auto-tunneling, lockdown and proxying."
        homepage = "https://wgtunnel.com"
        copyright = "MIT"
        fileAssociation(
            mimeType = "application/zip",
            extension = "zip",
            description = "ZIP Archive"
        )
        fileAssociation(
            mimeType = "text/plain",
            extension = "conf",
            description = "Configuration File"
        )

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

        // Include WinSW and XML
        appContent.from(stagePackagingSidecars)

        // Include the compiled GraalVM daemon
        appContent.from(
            fileTree(project(":daemon").layout.buildDirectory.dir("compose/tmp/main/graalvm/nativeCompile")) {
                include("wgtunnel-daemon", "wgtunnel-daemon.exe")
                builtBy(":daemon:nativeImageCompile")
            }
        )

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
            debDepends = listOf("systemd")
            rpmRequires = listOf("systemd")
            pacmanDepends = listOf("systemd")
            iconFile.set(rootProject.file("packaging/linux/icon.png"))
            afterInstall.set(rootProject.file("packaging/linux/after-install.sh"))
            afterRemove.set(rootProject.file("packaging/linux/after-remove.sh"))
            beforeInstall.set(rootProject.file("packaging/linux/before-install.sh"))
            beforeRemove.set(rootProject.file("packaging/linux/before-remove.sh"))
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
                installerLanguages = listOf(
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
    }
}

val graalvmLaunchersDir =
    layout.buildDirectory.dir("compose/binaries/main/graalvm-app/$appFsName")
val graalvmNativeCompileDirs =
    listOf(
        project(":daemon").layout.buildDirectory.dir("compose/tmp/main/graalvm/nativeCompile"),
    )
val graalvmLauncherNames = listOf("wgtunnel-daemon", "wgtunnel-daemon.exe")
val copyGraalvmSidecarDepends =
    listOf(
        ":daemon:nativeImageCompile",
        "copyGraalvmBinaryToOutput",
    )

val graalvmSidecarTaskNames = mutableListOf("copyGraalvmPackagingSidecars")

tasks.register<Copy>("copyGraalvmPackagingSidecars") {
    group = "nucleus"
    description = "Copy JVM packaging sidecars (service unit, WinSW) into the GraalVM app image."
    dependsOn(stagePackagingSidecars, "copyGraalvmBinaryToOutput")
    doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
    from(stagePackagingSidecars)
    into(graalvmLaunchersDir)
}

if (isWindows) {
    tasks.register<Copy>("copyGraalvmExtraLaunchersToRoot") {
        group = "nucleus"
        description = "Copy native daemon next to the GraalVM GUI binary (WinSW)."
        dependsOn(copyGraalvmSidecarDepends)
        doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
        graalvmNativeCompileDirs.forEach { from(it) }
        include(*graalvmLauncherNames.toTypedArray())
        into(graalvmLaunchersDir)
    }
    graalvmSidecarTaskNames += "copyGraalvmExtraLaunchersToRoot"
} else {
    tasks.register<Copy>("copyGraalvmExtraLaunchersToBin") {
        group = "nucleus"
        description = "Copy native daemon into the GraalVM app image bin/ directory."
        dependsOn(copyGraalvmSidecarDepends)
        doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
        graalvmNativeCompileDirs.forEach { from(it) }
        include(*graalvmLauncherNames.toTypedArray())
        into(graalvmLaunchersDir.map { it.dir("bin") })
    }
    graalvmSidecarTaskNames += "copyGraalvmExtraLaunchersToBin"
}

tasks.withType<Copy>().configureEach {
    if (name.startsWith("copyGraalvm")) {
        doNotTrackState("Shared graalvm-app dir is mutated by strip/patchelf")
    }
}

afterEvaluate {
    tasks.matching { it.name.startsWith("packageGraalvm") }.configureEach {
        dependsOn(graalvmSidecarTaskNames)
    }
}

tasks.named("copyNonXmlValueResourcesForJvmMain") { dependsOn("exportLibraryDefinitions") }
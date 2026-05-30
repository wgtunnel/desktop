import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.conveyor) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.licensee) apply false
}

val jvmVersion = libs.versions.jvm.get().toInt()
version = libs.versions.app.get()

allprojects {
    group = "com.zaneschepke.wireguardautotunnel"
    version = version
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(jvmVersion)
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            jvmToolchain(jvmVersion)
        }
    }
}

subprojects {
    apply {
        plugin(rootProject.libs.plugins.ktfmt.get().pluginId)
        plugin(rootProject.libs.plugins.aboutLibraries.get().pluginId)
    }

    tasks.register<KtfmtFormatTask>("format") {
        source = project.fileTree(rootDir)
        include("**/*.kt")
        exclude("**/build/**", ".*generated.*", "**/winsw/**", "**/amneziawg-tools/**", "**/.gradle/**")
    }

    aboutLibraries {
        collect {
            all = true
            includePlatform = true
        }
        export {
            outputFile = file("src/jvmMain/composeResources/files/aboutlibraries.json")
            prettyPrint = true
        }
    }

    ktfmt {
        kotlinLangStyle()
    }
}


registerConveyorTask("buildLinuxDeb",      "debian-package", "deb")
registerConveyorTask("buildWindowsMsix",   "windows-msix",   "windows")
registerConveyorTask("buildMacApp",        "mac-app",        "mac")
registerConveyorTask("buildMacZip",        "unnotarized-mac-zip", "mac")
registerConveyorTask("buildConveyorSite",  "copied-site",    "site")

// macOS PKG installer — bundles the .app and the launchd plist, with pre/postinstall scripts
// that register the daemon automatically. No manual install.sh required.
tasks.register<Exec>("buildMacPkg") {
    group = "distribution"
    description = "Builds a macOS .pkg installer with launchd daemon auto-registration"
    dependsOn("buildMacApp")

    val pkgRoot   = layout.buildDirectory.dir("mac-pkg/root")
    val appSrc    = layout.buildDirectory.dir("conveyor/mac")
    val plistSrc  = layout.projectDirectory.file("scripts/macos/pkg/com.zaneschepke.wgtunnel.daemon.plist")
    val pkgScripts = layout.projectDirectory.dir("scripts/macos/pkg")
    val output    = layout.buildDirectory.file("mac-pkg/WGTunnel.pkg")

    // Assemble the payload directory and invoke pkgbuild via a shell one-liner so the
    // Exec task has a single commandLine (required for configuration-cache compatibility).
    val appVersion = version.toString()
    commandLine(
        "bash", "-c", """
        set -e
        ROOT="${'$'}{1}"
        APP_SRC="${'$'}{2}"
        PLIST="${'$'}{3}"
        SCRIPTS="${'$'}{4}"
        OUTPUT="${'$'}{5}"
        VERSION="${'$'}{6}"

        rm -rf "${'$'}ROOT"
        mkdir -p "${'$'}ROOT/Applications" "${'$'}ROOT/Library/LaunchDaemons"

        APP=$(find "${'$'}APP_SRC" -maxdepth 1 -name '*.app' | head -1)
        [ -z "${'$'}APP" ] && { echo "No .app found in ${'$'}APP_SRC"; exit 1; }
        cp -r "${'$'}APP" "${'$'}ROOT/Applications/"

        cp "${'$'}PLIST" "${'$'}ROOT/Library/LaunchDaemons/"
        mkdir -p "$(dirname "${'$'}OUTPUT")"

        pkgbuild \
            --root            "${'$'}ROOT"    \
            --scripts         "${'$'}SCRIPTS" \
            --identifier      "com.zaneschepke.wgtunnel" \
            --version         "${'$'}VERSION" \
            --install-location "/" \
            "${'$'}OUTPUT"
        echo "macOS PKG built: ${'$'}OUTPUT"
        """.trimIndent(),
        "--",
        pkgRoot.get().asFile.absolutePath,
        appSrc.get().asFile.absolutePath,
        plistSrc.asFile.absolutePath,
        pkgScripts.asFile.absolutePath,
        output.get().asFile.absolutePath,
        appVersion
    )
}


registerConveyorTask("buildLinuxDebRelease",      "debian-package", "deb",      "conveyor-release.conf")
registerConveyorTask("buildWindowsMsixRelease",   "windows-msix",   "windows",  "conveyor-release.conf")
registerConveyorTask("buildMacZipRelease",        "unnotarized-mac-zip", "mac", "conveyor-release.conf")
registerConveyorTask("buildConveyorSiteRelease",  "copied-site",    "site",     "conveyor-release.conf")


tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
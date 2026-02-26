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
registerConveyorTask("buildConveyorSite",  "site",           "site")


registerConveyorTask("buildLinuxDebRelease",      "debian-package", "deb",      "conveyor-release.conf")
registerConveyorTask("buildWindowsMsixRelease",   "windows-msix",   "windows",  "conveyor-release.conf")
registerConveyorTask("buildConveyorSiteRelease",  "copied-site",           "site",     "conveyor-release.conf")


tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
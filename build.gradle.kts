// build.gradle.kts
plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.conveyor) apply false
    alias(libs.plugins.moko) apply false
    alias(libs.plugins.buildconfig) apply false
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

registerConveyorTask(
    taskName = "buildLinuxDeb",
    packageType = "debian-package",
    subDir = "deb",
)

registerConveyorTask(
    taskName = "buildWindowsMsix",
    packageType = "windows-msix",
    subDir = "windows",
)

registerConveyorTask(
    taskName = "buildConveyorSite",
    packageType = "site",
    subDir = "site"
)


tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.buildconfig)
}

group = "com.zaneschepke.wireguardautotunnel"

version = libs.versions.app.get()

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":client"))
            implementation(project(":parser"))
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
            implementation(libs.logback.classic)

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
        }
    }
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
}

buildConfig { buildConfigField("APP_VERSION", provider { "${project.version}" }) }

compose.desktop {
    application {
        mainClass = "com.zaneschepke.wireguardautotunnel.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage,
            )
            packageName = "com.zaneschepke.wireguardautotunnel.desktop"
            packageVersion = libs.versions.app.get()
        }
    }
}

// Conveyor
dependencies {
    linuxAmd64(libs.desktop.jvm.linux.x64)
    macAmd64(libs.desktop.jvm.macos.x64)
    macAarch64(libs.desktop.jvm.macos.arm64)
    windowsAmd64(libs.desktop.jvm.windows.x64)
    windowsAarch64(libs.desktop.jvm.windows.arm64)
}

tasks.named("copyNonXmlValueResourcesForJvmMain") {
    dependsOn("exportLibraryDefinitions")
}

tasks.named<Delete>("clean") { delete(file("generated.conveyor.conf")) }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":keyring"))
                api(project(":shared"))
                implementation(libs.wgtunnel.backend)
                implementation(libs.androidx.room3.runtime)
                implementation(libs.androidx.sqlite.bundled)

                implementation(libs.kermit)

                implementation(libs.kotlinx.serialization)

                // DI
                implementation(libs.koin.core)

                implementation(libs.bundles.ktor.client.jvm)
                implementation(libs.nucleus.native.http.ktor)

                // Util
                implementation(libs.apache.commons.lang3)
            }
        }
    }
}

dependencies { "kspJvm"(libs.androidx.room3.compiler) }

room3 { schemaDirectory("$projectDir/schemas") }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":parser"))
                implementation(project(":keyring"))
                api(project(":shared"))
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)

                implementation(libs.kermit)
                implementation(libs.logback.classic)

                implementation(libs.kotlinx.serialization)

                // DI
                implementation(libs.koin.core)

                implementation(libs.bundles.ktor.client.jvm)

                // Util
                implementation(libs.apache.commons.lang3)
            }
        }
    }
}

dependencies { "kspJvm"(libs.androidx.room.compiler) }

room { schemaDirectory("$projectDir/schemas") }

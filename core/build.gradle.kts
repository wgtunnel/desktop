plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.apache.commons.lang3)

    // Logging
    implementation(libs.kermit)
    implementation(libs.logback.classic)

    implementation(libs.kotlinx.coroutines.core)

    // Backoff
    implementation(libs.kotlin.retry)
}
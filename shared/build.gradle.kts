plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

dependencies {
    api(libs.wgtunnel.parser)
    implementation(libs.kotlinx.serialization)

    // Logging
    implementation(libs.kermit)

    implementation(libs.kotlinx.coroutines.core)

    // Backoff
    implementation(libs.kotlin.retry)
}

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

dependencies {
    api(libs.wgtunnel.parser)
    implementation(libs.kotlinx.serialization)
    implementation(libs.nucleus.core.runtime)

    // Logging
    implementation(libs.kermit)

    implementation(libs.kotlinx.coroutines.core)

    // Backoff
    implementation(libs.kotlin.retry)
}

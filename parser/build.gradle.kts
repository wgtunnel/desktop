plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.crypto.rand)
    implementation(libs.curve25519.kotlin)
}

tasks.test {
    useJUnitPlatform()
}
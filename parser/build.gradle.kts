plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.crypto.rand)
    implementation(libs.curve25519.kotlin)

    implementation("nl.jacobras:Human-Readable:1.12.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
}

tasks.test { useJUnitPlatform() }

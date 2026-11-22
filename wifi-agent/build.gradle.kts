import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization

plugins {
    kotlin("jvm")
    alias(libs.plugins.nucleus)
}

val agentIsMacOS = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

dependencies {
    implementation(project(":shared"))
    implementation(libs.wgtunnel.backend)

    implementation(libs.ktor.client.core.jvm)
    implementation(libs.ktor.client.cio.jvm)
    implementation(libs.ktor.client.content.negotiation.jvm)
    implementation(libs.ktor.client.websockets.jvm)
    implementation(libs.ktor.serialization.json.jvm)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kermit)

    testImplementation(kotlin("test"))
}

nucleus.application {
    mainClass = "com.zaneschepke.wireguardautotunnel.wifi.WifiAgentMainKt"
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Xms8m",
        "-Xmx32m",
        "-XX:ReservedCodeCacheSize=16m",
        "-Xss256k",
    )
    graalvm {
        // Linux and Windows packaging never depend on this image.
        isEnabled = agentIsMacOS
        imageName = "wgtunnel-wifi"
        optimization = NativeImageOptimization.SIZE
        headless = true
        maxHeapSize = "32m"
        buildArgs.add(
            "-Dapp.variant=${findProperty("app.variant") ?: System.getenv("WGTUNNEL_VARIANT") ?: "release"}",
        )
        buildArgs.addAll(GraalvmNativeArgs.daemon(System.getProperty("os.name").orEmpty()))
    }
    nativeDistributions {
        packageName = "wgtunnel-wifi"
        packageVersion = libs.versions.app.get()
    }
}

tasks.test { useJUnitPlatform() }

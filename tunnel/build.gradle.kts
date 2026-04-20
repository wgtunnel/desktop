plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jna.platform)

    implementation(libs.kermit)
    implementation(libs.logback.classic)
}

tasks.test {
    useJUnitPlatform()
}


tasks.register<Exec>("buildGoLibs") {
    val goDir = "tools/libwg-go"
    group = "build"
    description = "Builds Go shared libs using Makefile"
    workingDir = file(goDir)

    // doNotTrackState: outputs live on NFS and contain cross-platform artifacts built by
    // different hosts (Docker for Linux/Windows, native for macOS). Gradle must not delete
    // the output dir before re-running, as it cannot remove NFS-locked subdirs.
    doNotTrackState("Outputs are cross-platform artifacts on an NFS volume")

    // Use /tmp for BUILDDIR to avoid issues with network filesystems (NFS/SMB).
    // On macOS, restrict to darwin platforms only — Linux/Windows are built via Docker.
    val isMac = org.gradle.internal.os.OperatingSystem.current().isMacOsX
    val makeArgs = buildList {
        add("make")
        add("BUILDDIR=/tmp/wgtunnel-build-libwg")
        if (isMac) add("PLATFORMS=darwin-amd64 darwin-arm64")
        add("all")
    }
    commandLine(makeArgs)
}

tasks.named("processResources") {
    dependsOn("buildGoLibs")
}

val cleanGoLibs = tasks.register<Exec>("cleanGoLibs") {
    workingDir = file("tools/libwg-go")
    commandLine("make", "BUILDDIR=/tmp/wgtunnel-build-libwg", "clean")
}

// 3. Update the main clean task
tasks.named<Delete>("clean") {
    dependsOn(cleanGoLibs)
    delete(file("tools/libwg-go/build"))
    delete(file("tools/libwg-go/out"))
    delete(file("src/main/resources"))
}

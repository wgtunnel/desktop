plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(project(":tunnel"))
    implementation(project(":parser"))
    implementation(project(":shared"))

    // DI
    implementation(libs.koin.core)

    implementation(libs.bundles.ktor.server.jvm)

    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kermit)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))

    // caching
    implementation(libs.multiplatform.settings)

    implementation(libs.kotlinx.serialization)

    // Util
    implementation(libs.apache.commons.lang3)
}

application { mainClass.set("com.zaneschepke.wireguardautotunnel.daemon.MainKt") }

tasks.test { useJUnitPlatform() }

val cleanDotNet =
    tasks.register<Exec>("cleanDotNet") {
        group = "build"
        workingDir = file("winsw/src")
        commandLine("dotnet", "clean", "-c", "Release")
    }

tasks.named<Delete>("clean") {
    dependsOn(cleanDotNet)

    delete(file("output"))
    // Clean up WinSW specific artifacts
    delete(file("winsw/src/WinSW/bin"))
    delete(file("winsw/src/WinSW/obj"))
    delete(file("winsw/artifacts"))
}

tasks.named("installDist") {
    dependsOn("buildWinSW")
    dependsOn("buildWinSWArm64")
}

tasks.register<Exec>("buildWinSW") {
    val winSwDir = "winsw/src/WinSW"
    group = "build"
    description = "Build Windows service wrapper."
    workingDir = file(winSwDir)

    inputs
        .files(
            fileTree(winSwDir) {
                include("**/*.cs", "**/*.csproj", "**/appsettings.json")
                exclude("bin/**", "obj/**")
            }
        )
        .withPropertyName("winSwSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs
        .dir(file("$winSwDir/bin/Release/net7.0-windows/win-x64/publish"))
        .withPropertyName("winSwPublishDir")

    commandLine(
        "dotnet",
        "publish",
        "WinSW.csproj",
        "-f",
        "net7.0-windows",
        "-c",
        "Release",
        "-r",
        "win-x64",
        "--self-contained",
        "true",
        "-p:PublishSingleFile=true",
        // don't let new NuGet audit advisories fail the WinSW build
        "-p:WarningsNotAsErrors=NU1901%3BNU1902%3BNU1903%3BNU1904",
    )
}

tasks.register<Exec>("buildWinSWArm64") {
    val winSwDir = "winsw/src/WinSW"
    group = "build"
    description = "Build Windows service wrapper for ARM64."
    workingDir = file(winSwDir)

    inputs
        .files(
            fileTree(winSwDir) {
                include("**/*.cs", "**/*.csproj", "**/appsettings.json")
                exclude("bin/**", "obj/**")
            }
        )
        .withPropertyName("winSwSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs
        .dir(file("$winSwDir/bin/Release/net7.0-windows/win-arm64/publish"))
        .withPropertyName("winSwArm64PublishDir")

    commandLine(
        "dotnet",
        "publish",
        "WinSW.csproj",
        "-f",
        "net7.0-windows",
        "-c",
        "Release",
        "-r",
        "win-arm64",
        "--self-contained",
        "true",
        "-p:PublishSingleFile=true",
        "-p:PlatformTarget=arm64",
        "-p:WarningsNotAsErrors=NU1901%3BNU1902%3BNU1903%3BNU1904",
    )
}

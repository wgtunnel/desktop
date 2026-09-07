import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
    alias(libs.plugins.nucleus)
}

val daemonJvmArgs =
    listOf(
        "-XX:+UseSerialGC",
        "-Xms16m",
        "-Xmx96m",
        "-XX:ReservedCodeCacheSize=48m",
        "-Xss512k",
        "-Dkotlinx.coroutines.io.parallelism=16",
        "--enable-native-access=ALL-UNNAMED"
    )

dependencies {
    implementation(project(":shared"))
    implementation(libs.wgtunnel.backend)

    implementation(libs.bundles.ktor.server.jvm)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.kermit)

    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization)
}

nucleus.application {
    mainClass = "com.zaneschepke.wireguardautotunnel.daemon.MainKt"
    jvmArgs(*daemonJvmArgs.toTypedArray())
    graalvm {
        isEnabled = true
        imageName = "wgtunnel-daemon"
        optimization = NativeImageOptimization.SIZE
        headless = true
        maxHeapSize = "32m"
        buildArgs.addAll(GraalvmNativeArgs.daemon(System.getProperty("os.name").orEmpty()))
    }
    nativeDistributions {
        packageName = "wgtunnel-daemon"
        packageVersion = libs.versions.app.get()
    }
}

tasks.test { useJUnitPlatform() }

val printDevRunInfo =
    tasks.register("printDevRunInfo") {
        group = "application"
        description = "Prints the dev daemon run info."
        dependsOn(tasks.named("classes"))

        val runtimeClasspath = sourceSets.main.get().runtimeClasspath
        val javaHome = System.getProperty("java.home")
        val jvmArgsLine = daemonJvmArgs.joinToString(" ")

        doLast {
            println("$javaHome/bin/java")
            println(jvmArgsLine)
            println(runtimeClasspath.asPath)
        }
    }

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
    delete(file("winsw/artifacts"))
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
        .dir(file("winsw/artifacts/bin/WinSW/x64/Release/net7.0-windows/win-x64/publish"))
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
    )
}

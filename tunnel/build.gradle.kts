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

    // Track only source files
    inputs.files(
        fileTree(goDir) {
            include("**/*.go", "**/go.mod", "**/go.sum", "Makefile")
            exclude("out/**", "build/**", ".gocache/**")
        }
    ).withPropertyName("goSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(file("src/main/resources"))
        .withPropertyName("outputResourcesDir")

    commandLine("make", "all")
}

tasks.named("processResources") {
    dependsOn("buildGoLibs")
}

val cleanGoLibs = tasks.register<Exec>("cleanGoLibs") {
    workingDir = file("tools/libwg-go")
    commandLine("make", "clean")
}

// 3. Update the main clean task
tasks.named<Delete>("clean") {
    dependsOn(cleanGoLibs)
    delete(file("tools/libwg-go/build"))
    delete(file("tools/libwg-go/out"))
    delete(file("src/main/resources"))
}

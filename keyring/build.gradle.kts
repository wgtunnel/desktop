plugins { kotlin("jvm") }

dependencies { implementation(libs.jna.platform) }

tasks.register<Exec>("buildGoLibs") {
    val libDir = "tools/keyring-go"
    group = "build"
    description = "Builds Go shared libs using Makefile"
    workingDir = file(libDir)

    inputs
        .dir(file(libDir))
        .withPropertyName("goSourceDir")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(file("src/main/resources")).withPropertyName("outputResourcesDir")

    commandLine("make", "all")
}

tasks.named("processResources") { dependsOn("buildGoLibs") }

val cleanGoLibs =
    tasks.register<Exec>("cleanGoLibs") {
        workingDir = file("tools/keyring-go")
        commandLine("make", "clean")
    }

tasks.named<Delete>("clean") {
    dependsOn(cleanGoLibs)
    delete(file("tools/keyring-go/out"))
    delete(file("src/main/resources"))
}

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType

plugins { kotlin("jvm") }

dependencies { implementation(libs.nucleus.core.runtime) }

// CGO needs jni.h. Point Make at the same Temurin 25 toolchain Gradle compiles with.
val keyringJdkHome =
    extensions
        .getByType<JavaToolchainService>()
        .launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
        }
        .map { it.metadata.installationPath.asFile.absolutePath.replace('\\', '/') }

tasks.register<Exec>("buildGoLibs") {
    val goDir = "tools/keyring-go"
    group = "build"
    description = "Builds keyring JNI shared libs using Makefile"
    workingDir = file(goDir)

    inputs
        .files(
            fileTree(goDir) {
                include("**/*.go", "**/go.mod", "**/go.sum", "Makefile")
                exclude("out/**", "build/**", ".gocache/**")
            }
        )
        .withPropertyName("goSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(file("src/main/resources/nucleus/native")).withPropertyName("outputResourcesDir")

    environment("JAVA_HOME", keyringJdkHome)

    commandLine("make", "all")
}

tasks.named("processResources") { dependsOn("buildGoLibs") }

val cleanGoLibs =
    tasks.register<Exec>("cleanGoLibs") {
        workingDir = file("tools/keyring-go")
        commandLine("make", "clean")
        isIgnoreExitValue = true
    }

tasks.named<Delete>("clean") {
    dependsOn(cleanGoLibs)
    delete(file("tools/keyring-go/out"))
    delete(file("src/main/resources/nucleus/native"))
    delete(file("src/main/resources/natives"))
}

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType

plugins { kotlin("jvm") }

dependencies { implementation(libs.nucleus.core.runtime) }

val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

val keyringJdkHome =
    extensions
        .getByType<JavaToolchainService>()
        .launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
        }
        .map { it.metadata.installationPath.asFile.absolutePath.replace('\\', '/') }

tasks.register<BuildKeyringGoLibsTask>("buildGoLibs") {
    group = "build"
    description = "Builds keyring JNI shared libs"
    jdkHome.set(keyringJdkHome)
    windows.set(isWindows)
    goDir.set(layout.projectDirectory.dir("tools/keyring-go"))
    goSources.from(
        fileTree("tools/keyring-go") {
            include("**/*.go", "**/go.mod", "**/go.sum", "Makefile")
            exclude("out/**", "build/**", ".gocache/**")
        }
    )
    nativeOut.set(layout.projectDirectory.dir("src/main/resources/nucleus/native"))
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

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations

plugins { kotlin("jvm") }

dependencies { implementation(libs.nucleus.core.runtime) }

val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

// CGO needs jni.h. Point the build at the same JDK 25 Gradle compiles with.
val keyringJdkHome =
    extensions
        .getByType<JavaToolchainService>()
        .launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
        }
        .map { it.metadata.installationPath.asFile.absolutePath.replace('\\', '/') }

tasks.register("buildGoLibs") {
    val goDir = file("tools/keyring-go")
    group = "build"
    description = "Builds keyring JNI shared libs"
    val nativeOut = file("src/main/resources/nucleus/native")

    inputs
        .files(
            fileTree(goDir) {
                include("**/*.go", "**/go.mod", "**/go.sum", "Makefile")
                exclude("out/**", "build/**", ".gocache/**")
            }
        )
        .withPropertyName("goSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(nativeOut).withPropertyName("outputResourcesDir")

    val execOps = project.serviceOf<ExecOperations>()
    doLast {
        val jdk = keyringJdkHome.get()
        if (isWindows) {
            val outDir = file("$goDir/out")
            outDir.mkdirs()
            val outDll = file("$outDir/libkeyring-windows-amd64.dll")
            execOps.exec {
                workingDir = goDir
                environment("CGO_ENABLED", "1")
                environment("GOOS", "windows")
                environment("GOARCH", "amd64")
                environment("CC", "gcc")
                environment("CGO_CFLAGS", "-I$jdk/include -I$jdk/include/win32")
                commandLine(
                    "go",
                    "build",
                    "-v",
                    "-buildmode=c-shared",
                    "-trimpath",
                    "-ldflags=-buildid=",
                    "-o",
                    outDll.absolutePath,
                    ".",
                )
            }
            val dest = file("$nativeOut/win32-x64/keyring.dll")
            dest.parentFile.mkdirs()
            outDll.copyTo(dest, overwrite = true)
        } else {
            execOps.exec {
                workingDir = goDir
                environment("JAVA_HOME", jdk)
                commandLine("make", "all")
            }
        }
    }
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

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Invokes go/make; output is small")
abstract class BuildKeyringGoLibsTask @Inject constructor(private val execOps: ExecOperations) :
    DefaultTask() {
    @get:Input abstract val jdkHome: Property<String>

    @get:Input abstract val windows: Property<Boolean>

    @get:Internal abstract val goDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val goSources: ConfigurableFileCollection

    @get:OutputDirectory abstract val nativeOut: DirectoryProperty

    @TaskAction
    fun build() {
        val jdk = jdkHome.get()
        val go = goDir.get().asFile
        if (windows.get()) {
            val outDir = go.resolve("out")
            outDir.mkdirs()
            val outDll = outDir.resolve("libkeyring-windows-amd64.dll")
            execOps.exec {
                workingDir = go
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
            val dest = nativeOut.get().asFile.resolve("win32-x64/keyring.dll")
            dest.parentFile.mkdirs()
            outDll.copyTo(dest, overwrite = true)
        } else {
            execOps.exec {
                workingDir = go
                environment("JAVA_HOME", jdk)
                commandLine("make", "all")
            }
        }
    }
}

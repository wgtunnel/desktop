import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Small sidecar staging; not worth caching")
abstract class StagePackagingSidecarsTask @Inject constructor() : DefaultTask() {
    @get:Input abstract val appFsName: Property<String>

    @get:Input abstract val appDisplayName: Property<String>

    @get:Input abstract val windows: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxService: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val windowsServiceXml: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val wintunDll: RegularFileProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val winSwPublishDir: DirectoryProperty

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun stage() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        val fsName = appFsName.get()
        val display = appDisplayName.get()
        out.resolve("wgtunnel-daemon.service")
            .writeText(replaceTokens(linuxService.get().asFile.readText(), fsName, display))
        if (!windows.get()) return
        windowsServiceXml.orNull?.asFile?.let { xml ->
            out.resolve("service-wrapper.xml")
                .writeText(replaceTokens(xml.readText(), fsName, display))
        }
        wintunDll.orNull
            ?.asFile
            ?.takeIf { it.isFile }
            ?.copyTo(out.resolve("wintun.dll"), overwrite = true)
        val winSw = winSwPublishDir.orNull?.asFile ?: return
        sequenceOf("WinSW.exe", "WinSW-x64.exe")
            .map { winSw.resolve(it) }
            .firstOrNull { it.isFile }
            ?.copyTo(out.resolve("service-wrapper.exe"), overwrite = true)
    }

    private fun replaceTokens(text: String, fsName: String, display: String): String =
        text.replace("__APP_FSNAME__", fsName).replace("__APP_DISPLAY__", display)
}

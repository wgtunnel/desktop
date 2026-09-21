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

    @get:Input abstract val macOS: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val macDaemonBundleId: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val macDaemonPlist: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxService: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxInstallScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxUninstallScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxAfterInstallScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxBeforeInstallScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linuxBeforeRemoveScript: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val windowsServiceXml: RegularFileProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val winSwPublishDir: DirectoryProperty

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    // Separate from outputDir/common (which feeds appResourcesRootDir and gets bundled into the
    // app payload) since these are install-time hooks consumed directly by electron-builder/fpm.
    @get:OutputDirectory abstract val hooksOutputDir: DirectoryProperty

    @TaskAction
    fun stage() {
        // appResourcesRootDir requires a common/<os>/<target> layout (Nucleus copies
        // common/ contents to the app root, next to the native executable for GraalVM
        // packaging). Everything here is platform-agnostic at the tar/install-script level,
        // so it all lands under common.
        val root = outputDir.get().asFile
        root.deleteRecursively()
        val out = root.resolve("common")
        out.mkdirs()
        val fsName = appFsName.get()
        val display = appDisplayName.get()
        out.resolve("wgtunnel-daemon.service")
            .writeText(replaceTokens(linuxService.get().asFile.readText(), fsName, display))
        out.resolve("install.sh")
            .writeExecutable(
                replaceTokens(linuxInstallScript.get().asFile.readText(), fsName, display)
            )
        out.resolve("uninstall.sh")
            .writeExecutable(
                replaceTokens(linuxUninstallScript.get().asFile.readText(), fsName, display)
            )

        val hooks = hooksOutputDir.get().asFile
        hooks.deleteRecursively()
        hooks.mkdirs()
        hooks
            .resolve("before-install.sh")
            .writeExecutable(
                replaceTokens(linuxBeforeInstallScript.get().asFile.readText(), fsName, display)
            )
        hooks
            .resolve("before-remove.sh")
            .writeExecutable(
                replaceTokens(linuxBeforeRemoveScript.get().asFile.readText(), fsName, display)
            )
        // Pacman only calls after-upgrade (never after-install) when replacing an
        // already-installed package, so it needs its own copy of the same registration
        // logic
        hooks
            .resolve("after-upgrade.sh")
            .writeExecutable(
                linuxAfterInstallScript
                    .get()
                    .asFile
                    .readText()
                    .replace("\${executable}", fsName)
                    .replace("\${sanitizedProductName}", fsName)
            )

        if (macOS.get()) {
            val macOut = root.resolve("macos").apply { mkdirs() }
            macDaemonPlist.orNull?.asFile?.let { plist ->
                macOut
                    .resolve("${fsName}-daemon.plist")
                    .writeText(
                        replaceTokens(plist.readText(), fsName, display)
                            .replace(
                                "__DAEMON_BUNDLE_ID__",
                                macDaemonBundleId.getOrElse("com.wgtunnel.$fsName.daemon"),
                            )
                    )
            }
        }

        if (!windows.get()) return
        windowsServiceXml.orNull?.asFile?.let { xml ->
            out.resolve("service-wrapper.xml")
                .writeText(replaceTokens(xml.readText(), fsName, display))
        }
        val winSw = winSwPublishDir.orNull?.asFile ?: return
        sequenceOf("WinSW.exe", "WinSW-x64.exe")
            .map { winSw.resolve(it) }
            .firstOrNull { it.isFile }
            ?.copyTo(out.resolve("service-wrapper.exe"), overwrite = true)
    }

    private fun replaceTokens(text: String, fsName: String, display: String): String =
        text.replace("__APP_FSNAME__", fsName).replace("__APP_DISPLAY__", display)

    private fun java.io.File.writeExecutable(text: String) {
        writeText(text)
        setExecutable(true, false)
    }
}

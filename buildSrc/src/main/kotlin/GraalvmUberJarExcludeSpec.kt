import java.io.File
import java.io.Serializable
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.Jar

object GraalvmUberJarDrops {
    private val signatureExts = setOf("SF", "DSA", "RSA", "EC")
    private val properties =
        setOf(
            "META-INF/native-image/okhttp/okhttp/native-image.properties",
            "META-INF/native-image/dev.nucleusframework/composenativetray/native-image.properties",
        )

    fun shouldDrop(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        if (normalized.startsWith("META-INF/") && '/' !in normalized.removePrefix("META-INF/")) {
            val ext = normalized.substringAfterLast('.', missingDelimiterValue = "").uppercase()
            if (ext in signatureExts) return true
        }
        return normalized.endsWith("proxy-config.json") || normalized in properties
    }
}

class GraalvmUberJarExcludeSpec : Spec<FileTreeElement>, Serializable {
    override fun isSatisfiedBy(element: FileTreeElement): Boolean =
        GraalvmUberJarDrops.shouldDrop(element.path)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Nucleus copies the flattened zip with a child spec that ignores Copy `exclude()`. */
class StripGraalvmUberJarAction : Action<Task>, Serializable {
    override fun execute(task: Task) {
        val archive = (task as Jar).archiveFile.get().asFile
        if (!archive.isFile) return
        val tmp = File(archive.parentFile, "${archive.name}.stripped")
        ZipInputStream(archive.inputStream().buffered()).use { input ->
            ZipOutputStream(tmp.outputStream().buffered()).use { output ->
                var entry = input.nextEntry
                while (entry != null) {
                    if (!GraalvmUberJarDrops.shouldDrop(entry.name)) {
                        output.putNextEntry(ZipEntry(entry.name))
                        input.copyTo(output)
                        output.closeEntry()
                    }
                    entry = input.nextEntry
                }
            }
        }
        tmp.copyTo(archive, overwrite = true)
        tmp.delete()
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

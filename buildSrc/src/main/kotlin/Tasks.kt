import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

fun Project.registerConveyorTask(
    taskName: String,
    packageType: String,
    subDir: String,
    configFile: String = "conveyor-local.conf",
) {
    tasks.register<Exec>(taskName) {
        group = "distribution"
        val outputDir = layout.buildDirectory.dir("conveyor/$subDir")
        outputs.dir(outputDir)

        (System.getenv("CONVEYOR_SIGNING_KEY") ?: LocalProperties.get("conveyor.signing-key"))?.let {
            environment("CONVEYOR_SIGNING_KEY", it)
        }

        (System.getenv("CONVEYOR_PAT") ?: LocalProperties.get("github.pat"))?.let {
            environment("CONVEYOR_PAT", it)
        }

        val args =
            mutableListOf(
                "conveyor",
                "-f",
                configFile,
                "make",
                "--output-dir",
                outputDir.get().asFile.absolutePath,
                packageType,
            )

        LocalProperties.get("conveyor.passphrase")?.let {
            environment("CONVEYOR_PASSPHRASE", it)
            args.add(1, "--passphrase=env:CONVEYOR_PASSPHRASE")
        }

        commandLine(args)

        dependsOn(
            ":composeApp:createDistributable",
            ":cli:installDist",
            ":daemon:installDist",
            ":composeApp:writeConveyorConfig",
        )
    }
}

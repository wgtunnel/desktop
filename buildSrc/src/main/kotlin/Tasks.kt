import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

fun Project.registerConveyorTask(
    taskName: String,
    packageType: String,
    subDir: String,
    configFile: String = "conveyor.conf",
    signingKeyEnv: String? = null,
) {
    tasks.register<Exec>(taskName) {
        group = "distribution"
        val outputDir = layout.buildDirectory.dir("conveyor/$subDir")
        outputs.dir(outputDir)

        val args =
            mutableListOf(
                "conveyor",
                "-f",
                configFile,
                "make",
                "--output-dir",
                outputDir.get().asFile.absolutePath,
                packageType,
                "--rerun=all"
            )

        if (signingKeyEnv == null) {
            // dev builds use passphrase
            environment(
                "CONVEYOR_PASSPHRASE",
                System.getenv("CONVEYOR_PASSPHRASE")
                    ?: LocalProperties.get("conveyor.passphrase")
                    ?: "",
            )
            args.add(1, "--passphrase=env:CONVEYOR_PASSPHRASE")
        } else {
            // release builds use raw signing key
            environment("CONVEYOR_SIGNING_KEY", System.getenv(signingKeyEnv) ?: "")
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

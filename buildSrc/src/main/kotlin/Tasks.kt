import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

fun Project.registerConveyorTask(taskName: String, packageType: String, subDir: String) {
    tasks.register<Exec>(taskName) {
        group = "distribution"
        val outputDir = layout.buildDirectory.dir("conveyor/$subDir")
        outputs.dir(outputDir)

        environment(
            "CONVEYOR_PASSPHRASE",
            SystemVar.fromEnvironment("CONVEYOR_PASSPHRASE")
                ?: LocalProperties.get("conveyor.passphrase")
                ?: "",
        )

        val args =
            mutableListOf(
                "conveyor",
                "--passphrase=env:CONVEYOR_PASSPHRASE",
                "make",
                "--output-dir",
                outputDir.get().asFile.absolutePath,
                packageType,
            )

        commandLine(args)

        dependsOn(
            ":composeApp:createDistributable",
            ":cli:installDist",
            ":daemon:installDist",
            ":composeApp:writeConveyorConfig",
        )
    }
}

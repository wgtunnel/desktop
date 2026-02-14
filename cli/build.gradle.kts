plugins {
    application
    alias(libs.plugins.serialization)
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(project(":client"))
    // CLI
    implementation(libs.picocli)
    kapt(libs.picocli.codegen)

    // DI
    implementation(libs.koin.core)

    // Logging
    implementation(libs.kermit)
    implementation(libs.logback.classic)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.core)
}

kapt { arguments { arg("project", "${project.group}/${project.name}") } }

tasks.named<Sync>("installDist") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

application { mainClass.set("com.zaneschepke.wireguardautotunnel.cli.MainKt") }

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to libs.versions.app.get(),
            "Main-Class" to application.mainClass.get(),
        )
    }
}

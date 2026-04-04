import java.util.Properties

rootProject.name = "wgtunnel"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.hq.hydraulic.software")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.gradleup.nmcp.settings").version("1.4.4")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

fun localProperty(name: String): String? {
    val props = Properties()
    val file = rootDir.resolve("local.properties")

    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }

    return props.getProperty(name) ?: System.getenv(name)
}

nmcpSettings {
    centralPortal {
        username = localProperty("MAVEN_CENTRAL_USER")
        password = localProperty("MAVEN_CENTRAL_PASS")
        publishingType = "AUTOMATIC"
    }
}

include(":composeApp", ":parser", ":daemon", ":tunnel", ":cli", ":client", ":keyring", ":shared")

rootProject.name = "wgtunnel"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
  }
}

// Local dev
includeBuild("../core") {
  dependencySubstitution {
    substitute(module("com.wgtunnel:backend")).using(project(":backend"))
    substitute(module("com.wgtunnel:parser")).using(project(":parser"))
    substitute(module("com.wgtunnel:hevtunnel")).using(project(":hevtunnel"))
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

include(":composeApp", ":daemon", ":client", ":keyring", ":shared")

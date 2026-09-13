import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.gradle.api.plugins.JavaApplication
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  alias(libs.plugins.composeHotReload) apply false
  alias(libs.plugins.jetbrainsCompose) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.buildconfig) apply false
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.aboutLibraries)
  alias(libs.plugins.licensee) apply false
}

val jvmVersion = libs.versions.jvm.get().toInt()

version = libs.versions.app.get()

allprojects {
  group = "com.zaneschepke.wireguardautotunnel"
  version = version
  plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
      jvmToolchain { languageVersion.set(JavaLanguageVersion.of(jvmVersion)) }
    }
  }

  plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
      jvmToolchain { languageVersion.set(JavaLanguageVersion.of(jvmVersion)) }
    }
  }
}

subprojects {
  apply {
    plugin(rootProject.libs.plugins.ktfmt.get().pluginId)
    plugin(rootProject.libs.plugins.aboutLibraries.get().pluginId)
  }

  tasks.register<KtfmtFormatTask>("format") {
    description = "Kotlin formatting"
      source = project.fileTree(rootDir)
    include("**/*.kt")
    exclude("**/build/**", ".*generated.*", "**/winsw/**", "**/amneziawg-tools/**", "**/.gradle/**")
  }

  aboutLibraries {
    collect {
      all = true
      includePlatform = true
    }
    export {
      outputFile = file("src/jvmMain/composeResources/files/aboutlibraries.json")
      prettyPrint = true
    }
  }

  ktfmt { kotlinLangStyle() }

  // Signed dependency manifests (BouncyCastle, etc.) break GraalVM native-image
  // after they are merged into Nucleus's uber JAR. Pattern exclude() on zipTree
  // does not apply; a serializable Spec does.
  tasks.withType<Jar>().configureEach {
    if (name.contains("UberJar", ignoreCase = true)) {
      exclude(GraalvmUberJarExcludeSpec())
      doLast(StripGraalvmUberJarAction())
    }
  }

  pluginManager.withPlugin("application") {
    extensions.configure<JavaApplication> {
        applicationDefaultJvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
    }
  }

  tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
  }
}

tasks.register<Delete>("clean") { description = "Clean"
    delete(layout.buildDirectory) }

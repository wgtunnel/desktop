plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
    `maven-publish`
    signing
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.crypto.rand)
    implementation(libs.curve25519.kotlin)

    implementation(libs.human.readable)
    implementation(libs.kotlinx.datetime)
}

tasks.test { useJUnitPlatform() }

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.zaneschepke.wireguardautotunnel"
            artifactId = "amneziawg-parser"
            version = "1.0.1"
            from(components["java"])
            pom {
                name.set("AmneziaWG Parser")
                description.set("An AmneziaWG parser")
                url.set("https://wgtunnel.com/")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/wgtunnel/desktop.git")
                    developerConnection.set("scm:git:ssh://github.com/wgtunnel/desktop.git")
                    url.set("https://github.com/wgtunnel/desktop")
                }
                developers {
                    developer {
                        name.set("Zane Schepke")
                        email.set("support@zaneschepke.com")
                    }
                }
            }
        }
    }
}


signing {
    useInMemoryPgpKeys(
        LocalProperties.get("SECRET_KEY") ?: System.getenv("SECRET_KEY"),
        LocalProperties.get("PASSWORD") ?: System.getenv("PASSWORD")
    )
    sign(publishing.publications)
}

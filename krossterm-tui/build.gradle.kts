import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(25)
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
    }
}

dependencies {
    api(project(":krossterm"))

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.assertions.core)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets.all {
                testTask.configure {
                    useJUnitPlatform()
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("krossterm-tui")
                description.set("TUI helpers built on top of krossterm — higher-level building blocks for terminal UIs.")
                url.set("https://github.com/bryansant/krossterm")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("krossterm")
                        name.set("krossterm contributors")
                    }
                }
                scm {
                    url.set("https://github.com/bryansant/krossterm")
                    connection.set("scm:git:git://github.com/bryansant/krossterm.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bryansant/krossterm.git")
                }
            }
        }
    }
}

signing {
    val key = System.getenv("GPG_PRIVATE_KEY")
    val pwd = System.getenv("GPG_PASSPHRASE")
    if (!key.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pwd)
        sign(publishing.publications["maven"])
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

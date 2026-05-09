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
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.io.core)
    implementation(libs.jline.terminal)
    runtimeOnly(libs.jline.terminal.ffm)

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
                    systemProperty("kotest.tags.exclude", System.getProperty("kotest.tags.exclude", "Tty"))
                }
            }
        }
    }
}

tasks.register<Test>("ttyTest") {
    description = "Run TTY-required integration tests."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("kotest.tags.include", "Tty")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("krossterm")
                description.set("Pure-Kotlin reimplementation of crossterm — terminal manipulation for the JVM.")
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

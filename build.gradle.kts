import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
    id("com.gradleup.nmcp") version "1.4.4"
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

val examples: SourceSet by sourceSets.creating {
    java.srcDir("examples/src/main/java")
    kotlin.srcDir("examples/src/main/kotlin")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val examplesImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get(), configurations.runtimeOnly.get())
}

dependencies {
    "examplesImplementation"(libs.kotlinx.coroutines.core)
    "examplesImplementation"(libs.jline.terminal)
}

tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Run an example: ./gradlew runExample -Pexample=KeyDisplay"
    dependsOn("compileExamplesKotlin", "compileExamplesJava")
    classpath = examples.runtimeClasspath
    val name = providers.gradleProperty("example").orElse("InteractiveDemo")
    // Kotlin top-level functions compile to a class with a Kt suffix; Java classes keep their name.
    mainClass.set(name.map { n ->
        if (n.startsWith("Java")) "io.github.krossterm.examples.$n"
        else "io.github.krossterm.examples.${n}Kt"
    })
    standardInput = System.`in`
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
                url.set("https://github.com/krossterm/krossterm")
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
                    url.set("https://github.com/krossterm/krossterm")
                    connection.set("scm:git:git://github.com/krossterm/krossterm.git")
                    developerConnection.set("scm:git:ssh://git@github.com/krossterm/krossterm.git")
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

nmcp {
    publishAllPublicationsToCentralPortal {
        username = System.getenv("MVN_CENTRAL_USER") ?: ""
        password = System.getenv("MVN_CENTRAL_PASS") ?: ""
        publishingType = "AUTOMATIC"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

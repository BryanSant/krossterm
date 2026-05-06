import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.krossterm"
version = "0.1.0-SNAPSHOT"

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
    java.setSrcDirs(emptyList<File>())
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
    classpath = examples.runtimeClasspath
    val name = providers.gradleProperty("example").orElse("InteractiveDemo")
    mainClass.set(name.map { "io.github.krossterm.examples.${it}Kt" })
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
    repositories {
        maven {
            name = "sonatypeCentral"
            val isSnapshot = version.toString().endsWith("SNAPSHOT")
            url = uri(
                if (isSnapshot) "https://central.sonatype.com/repository/maven-snapshots/"
                else "https://central.sonatype.com/api/v1/publisher/upload/"
            )
            credentials {
                username = providers.gradleProperty("mavenCentralUsername").orNull
                    ?: System.getenv("MAVEN_CENTRAL_USERNAME")
                password = providers.gradleProperty("mavenCentralPassword").orNull
                    ?: System.getenv("MAVEN_CENTRAL_PASSWORD")
            }
        }
    }
}

signing {
    val key = providers.gradleProperty("signingInMemoryKey").orNull
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    val pwd = providers.gradleProperty("signingInMemoryKeyPassword").orNull
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    if (!key.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pwd)
        sign(publishing.publications["maven"])
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

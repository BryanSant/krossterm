import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(25)
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
    implementation(project(":krossterm"))
    implementation(project(":krossterm-tui"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jline.terminal)
}

tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Run an example: ./gradlew :examples:runExample -Pexample=KeyDisplay"
    classpath = sourceSets.main.get().runtimeClasspath
    val name = providers.gradleProperty("example").orElse("InteractiveDemo")
    mainClass.set(name.map { n ->
        if (n.startsWith("Java")) "io.github.krossterm.examples.$n"
        else "io.github.krossterm.examples.${n}Kt"
    })
    standardInput = System.`in`
}

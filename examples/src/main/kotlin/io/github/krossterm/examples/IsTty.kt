package io.github.krossterm.examples

import io.github.krossterm.Terminal

/**
 * Detects whether stdout is attached to a real TTY and prints the terminal type.
 *
 * Run with: `./gradlew runExample -Pexample=IsTty`
 */
fun main() {
    Terminal.system().use { t ->
        val jline = t.underlying
        val type = jline.type ?: "unknown"
        val (cols, rows) = t.size.let { it.columns to it.rows }
        println("Type: $type")
        println("Size: ${cols}x${rows}")
        println("System TTY: ${jline.javaClass.simpleName}")
    }
}

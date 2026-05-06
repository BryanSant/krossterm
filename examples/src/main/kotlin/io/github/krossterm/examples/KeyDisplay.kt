package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.event.Event
import io.github.krossterm.event.KeyCode
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking

/**
 * Read events from the terminal in raw mode and print each one.
 * Press Esc to exit.
 *
 * Run with: `./gradlew runExample -Pexample=KeyDisplay`
 */
fun main() {
    Terminal.system().use { t ->
        t.rawMode {
            println("Press keys (Esc to quit)...")
            runBlocking {
                t.events()
                    .takeWhile { it !is Event.Key || it.event.code != KeyCode.Esc }
                    .collect { ev -> println("event: $ev") }
            }
        }
    }
}

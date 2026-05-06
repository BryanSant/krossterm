package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.event.Event
import io.github.krossterm.event.KeyCode
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Demonstrate `pollEvent(timeout)` with a periodic tick: each second prints a
 * heartbeat; whenever a real event arrives, it's printed inline. Press Esc to
 * exit.
 *
 * Run with: `./gradlew runExample -Pexample=EventStreamCoroutines`
 */
fun main() {
    Terminal.system().use { t ->
        t.rawMode {
            println("Heartbeat + events (Esc quits)...")
            runBlocking {
                var ticks = 0
                while (true) {
                    val ev = t.pollEvent(1.seconds)
                    if (ev == null) {
                        println("tick ${++ticks}")
                        continue
                    }
                    println("event: $ev")
                    if (ev is Event.Key && ev.event.code == KeyCode.Esc) break
                }
            }
        }
    }
}

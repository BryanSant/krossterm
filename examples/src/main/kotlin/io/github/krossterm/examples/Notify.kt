package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.execute
import io.github.krossterm.SendNotification
import io.github.krossterm.style.bold
import io.github.krossterm.style.cyan
import io.github.krossterm.style.green
import io.github.krossterm.style.yellow

/**
 * Demonstrate desktop notifications via OSC 9 / OSC 99 / OSC 777.
 *
 * Each sequence targets a different terminal family:
 *   OSC 9   -- ConEmu, Windows Terminal
 *   OSC 99  -- Kitty
 *   OSC 777 -- urxvt + libnotify
 *
 * All three are emitted together so whichever your terminal supports fires.
 *
 * Run with: `./gradlew runExample -Pexample=Notify`
 */
fun main() {
    Terminal.system().use { t ->
        println("OSC notification demo".bold().toString())
        println("Sending three notifications with a 2-second pause between each.")
        println("If your terminal supports desktop notifications you should see them.")
        println()

        data class Demo(val title: String, val body: String, val label: String)
        val demos = listOf(
            Demo("Build started",  "Compiling krossterm...",     "cyan"),
            Demo("Tests passed",   "All 147 assertions green.",  "green"),
            Demo("Deploy complete","Production updated to v1.2.", "yellow"),
        )

        for ((title, body, color) in demos) {
            val label = when (color) {
                "green"  -> "[$title] $body".green()
                "yellow" -> "[$title] $body".yellow()
                else     -> "[$title] $body".cyan()
            }
            println("  Sending: $label")
            t.out.execute(SendNotification(title, body))
            Thread.sleep(2000)
        }

        println()
        println("Done.".green().toString())
    }
}

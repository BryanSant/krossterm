package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.execute
import io.github.krossterm.ClearProgress
import io.github.krossterm.ProgressState
import io.github.krossterm.SetProgress
import io.github.krossterm.style.green
import io.github.krossterm.style.red
import io.github.krossterm.style.yellow

/**
 * Demonstrate OSC 9;4 taskbar/tab progress reporting.
 *
 * Watch your terminal's tab or taskbar for a progress indicator while this runs.
 * Supported by Windows Terminal, WezTerm, iTerm2, and ConEmu.
 *
 * Run with: `./gradlew runExample -Pexample=Progress`
 */
fun main() {
    Terminal.system().use { t ->
        println("OSC 9;4 progress demo -- watch your terminal tab or taskbar.")
        println()

        // Normal progress: count from 0 to 100
        println("Normal (0 -> 100%)...")
        for (pct in 0..100 step 5) {
            t.out.execute(SetProgress(ProgressState.Normal(pct)))
            print("\r  ${"\u2588".repeat(pct / 5).padEnd(20)} $pct%  ")
            System.out.flush()
            Thread.sleep(80)
        }
        println()

        // Error state
        println("Error state at 60%...".red().toString())
        t.out.execute(SetProgress(ProgressState.Error(60)))
        Thread.sleep(1500)

        // Paused state
        println("Paused state at 75%...".yellow().toString())
        t.out.execute(SetProgress(ProgressState.Paused(75)))
        Thread.sleep(1500)

        // Indeterminate
        println("Indeterminate (busy spinner)...")
        t.out.execute(SetProgress(ProgressState.Indeterminate))
        Thread.sleep(2000)

        // Clear
        t.out.execute(ClearProgress)
        println("Done -- progress cleared.".green().toString())
    }
}

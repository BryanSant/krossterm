package io.github.krossterm.examples

import io.github.krossterm.style.bold
import io.github.krossterm.style.cyan
import io.github.krossterm.style.green
import io.github.krossterm.style.red
import io.github.krossterm.style.yellow
import io.github.krossterm.tui.Align
import io.github.krossterm.tui.BorderStyle
import io.github.krossterm.tui.ProgressStyle
import io.github.krossterm.tui.SpinnerStyle
import io.github.krossterm.tui.column
import io.github.krossterm.tui.divider
import io.github.krossterm.tui.frame
import io.github.krossterm.tui.println
import io.github.krossterm.tui.printlnFitted
import io.github.krossterm.tui.progressBar
import io.github.krossterm.tui.row
import io.github.krossterm.tui.table
import io.github.krossterm.tui.text
import io.github.krossterm.tui.withSpinner
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Full tour of krossterm-tui: frames, tables, layouts, progress bars, and a
 * coroutine spinner. Run with `./gradlew :examples:runExample -Pexample=TuiShowcase`.
 */
public fun main() {
    val banner = frame("krossterm-tui showcase") {
        borderStyle = BorderStyle.Double
        padding = 1
        +text("Built on ${"krossterm".cyan().bold()} — same DSL feel.", Align.Center)
    }
    banner.printlnFitted()

    println()

    val people = table {
        borderStyle = BorderStyle.Rounded
        headers("Name", "Role", "Status")
        align(Align.Start, Align.Start, Align.Center)
        row("Alice", "Engineer", "${"on call".green()}")
        row("Bob", "Designer", "${"away".yellow()}")
        row("Charlie", "PM", "${"offline".red()}")
    }
    people.println()

    println()

    val sideBySide = row {
        cell(weight = 1) {
            frame("Stats") {
                borderStyle = BorderStyle.BoxDraw
                +text("Builds  : 1,204")
                +text("Failures:    12")
                +text("Coverage:   89%")
            }
        }
        fixed(2, text(" "))
        cell(weight = 1) {
            frame("Hosts") {
                borderStyle = BorderStyle.BoxDraw
                +text("api-01 ${"healthy".green()}")
                +text("api-02 ${"healthy".green()}")
                +text("worker ${"down".red()}")
            }
        }
    }
    sideBySide.println()

    println()

    divider("Progress").printlnFitted()
    column {
        +progressBar(value = 12, total = 100, label = "Compiling", style = ProgressStyle.Blocks)
        +progressBar(value = 47, total = 100, label = "Bundling ", style = ProgressStyle.Smooth)
        +progressBar(value = 91, total = 100, label = "Uploading", style = ProgressStyle.Ascii)
    }.println()

    println()

    divider("Coroutine spinner").printlnFitted()
    runBlocking {
        withSpinner("Fetching widgets…", style = SpinnerStyle.Dots) { spinner ->
            delay(900)
            spinner.label = "Parsing payload…"
            delay(900)
            spinner.label = "Wiring it up…"
            delay(900)
        }
    }
    println("✓ done.")
}

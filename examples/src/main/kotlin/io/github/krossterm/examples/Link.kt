package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.execute
import io.github.krossterm.hyperlink.EndHyperlink
import io.github.krossterm.hyperlink.StartHyperlink
import io.github.krossterm.style.Print

/**
 * Print clickable OSC 8 hyperlinks. Try this in iTerm2, Kitty, WezTerm, GNOME
 * Terminal, or any other OSC 8-supporting terminal.
 *
 * Run with: `./gradlew runExample -Pexample=Link`
 */
fun main() {
    Terminal.system().use { t ->
        t.out.execute(
            Print("Click here: "),
            StartHyperlink("https://kotlinlang.org"),
            Print("Kotlin"),
            EndHyperlink,
            Print("\n"),
        )
    }
}

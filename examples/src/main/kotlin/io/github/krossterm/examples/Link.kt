package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.execute
import io.github.krossterm.link.EndLink
import io.github.krossterm.link.StartLink
import io.github.krossterm.link.link
import io.github.krossterm.style.Print
import io.github.krossterm.style.bold
import io.github.krossterm.style.cyan
import io.github.krossterm.style.underlined
import io.github.krossterm.terminal

/**
 * OSC 8 link examples — imperative, DSL, .link() extension, and params.
 * Try this in iTerm2, Kitty, WezTerm, GNOME Terminal, foot, or any other
 * OSC 8-supporting terminal.
 *
 * Run with: `./gradlew runExample -Pexample=Link`
 */
fun main() {
    Terminal.system().use { t ->

        // ── 1. Imperative style: t.out.execute() ─────────────────────────────
        // Pass StartLink, the visible content, and EndLink as a single vararg
        // batch — queued and flushed in one call.
        t.out.execute(
            Print("Imperative:  "),
            StartLink("https://kotlinlang.org"),
            Print("Kotlin".underlined()),
            EndLink,
            Print("\n"),
        )

        // ── 2. DSL style: t.out.terminal { } ─────────────────────────────────
        // StartLink and EndLink have no TerminalScope shorthand, so use the
        // + operator — syntactic sugar for Command.writeAnsi(out).
        t.out.terminal {
            print("DSL:         ")
            +StartLink("https://kotlinlang.org")
            print("Kotlin".underlined())
            +EndLink
            println()
        }

        // ── 3. .link() extension ──────────────────────────────────────────────
        // String.link() and StyledContent.link() return LinkContent whose
        // toString() emits the full OSC 8 sequence.  Compose style first, then
        // wrap with .link() — the SGR codes sit inside the OSC 8 boundaries.
        println("Extension:   ${"Kotlin".link("https://kotlinlang.org")}")
        println("Styled:      ${"Kotlin".bold().cyan().underlined().link("https://kotlinlang.org")}")

        // ── 4. With params (id=) ─────────────────────────────────────────────
        // The id param coalesces multiple segments into one clickable target —
        // useful when a link spans a line break.
        t.out.terminal {
            print("With id:     ")
            +StartLink("https://kotlinlang.org", mapOf("id" to "kt"))
            print("line one".underlined())
            +EndLink
            print("\n             ")
            +StartLink("https://kotlinlang.org", mapOf("id" to "kt"))
            print("line two (same link)".underlined())
            +EndLink
            println()
        }
    }
}

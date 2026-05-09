package io.github.krossterm.tui

import io.github.krossterm.TerminalScope
import io.github.krossterm.Terminal as KTerminal

/**
 * True when the JVM is attached to an interactive TTY for stdin and stdout.
 * Returns false when either stream is redirected (pipes, file output, CI, IDE
 * run consoles). Used by [withSpinner] to decide whether to animate.
 */
public fun isInteractive(): Boolean = System.console() != null

/**
 * True when the [NO_COLOR](https://no-color.org/) environment variable is set
 * to any non-empty value. Output paths in this library default to stripping
 * SGR (color/attribute) escape sequences when this is true — cursor movement
 * and hyperlink escapes are preserved, so the spinner still animates and OSC
 * 8 links still work; only color/style is removed.
 *
 * Pass `stripColor = false` to a specific output call to override per-call.
 */
public fun noColor(): Boolean =
    System.getenv("NO_COLOR")?.isNotEmpty() == true

/**
 * The current terminal width in columns, or [fallback] if it can't be
 * determined (e.g. stdout is a pipe). Queries jline via krossterm's
 * [Terminal][io.github.krossterm.Terminal]; safe to call from non-TTY
 * processes.
 *
 * This opens a transient terminal handle on each call — fine for the typical
 * "render once" CLI use case, but if you're rendering on every keystroke,
 * cache the result yourself and refresh on `SIGWINCH`.
 */
public fun terminalWidth(fallback: Int = 80): Int =
    try {
        KTerminal.system().use { t ->
            t.size.columns.takeIf { it > 0 } ?: fallback
        }
    } catch (_: Throwable) {
        System.getenv("COLUMNS")?.toIntOrNull()?.takeIf { it > 0 } ?: fallback
    }

/**
 * Print this component to stdout, sized to the live terminal width (or
 * [fallback] if it can't be determined). Components that prefer a width
 * larger than the terminal will be rendered at the terminal width instead.
 */
public fun Component.printlnFitted(fallback: Int = 80) {
    val width = terminalWidth(fallback)
    println(out = System.out, width = width)
}

/**
 * Render [component] inside `terminal { }` at the live terminal width
 * (or [fallback] if it can't be determined). Same trade-off as
 * [printlnFitted] for components that prefer a wider rendering.
 */
public fun TerminalScope.renderFitted(
    component: Component,
    fallback: Int = 80,
) {
    render(component, terminalWidth(fallback))
}

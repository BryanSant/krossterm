package io.github.krossterm.tui

import io.github.krossterm.TerminalScope
import java.io.PrintStream
import java.io.Writer

/**
 * Write this component's rendered lines to [out], joined with `\n`. The
 * caller is responsible for flushing; use [println] for a fire-and-forget
 * variant that flushes for you.
 *
 * If [stripColor] is true (the default when `NO_COLOR` is set), SGR escapes
 * are removed from the output. Other escapes (cursor motion, hyperlinks)
 * pass through unchanged.
 */
public fun Component.writeTo(
    out: Writer,
    width: Int = preferredWidth(),
    stripColor: Boolean = noColor(),
) {
    for ((i, line) in render(width).withIndex()) {
        if (i > 0) out.append('\n')
        out.append(if (stripColor) stripSgr(line) else line)
    }
}

/**
 * Render this component to a string at [preferredWidth] (or [width] if given).
 * SGR escapes are stripped when [stripColor] is true (default: `NO_COLOR`).
 */
public fun Component.toRenderedString(
    width: Int = preferredWidth(),
    stripColor: Boolean = noColor(),
): String {
    val joined = render(width).joinToString("\n")
    return if (stripColor) stripSgr(joined) else joined
}

/** Print this component to [out] followed by a newline; flushes on completion. */
public fun Component.println(
    out: Writer = System.out.writer(),
    width: Int = preferredWidth(),
    stripColor: Boolean = noColor(),
) {
    writeTo(out, width, stripColor)
    out.append('\n')
    out.flush()
}

/** Print this component to [out] followed by a newline; flushes on completion. */
public fun Component.println(
    out: PrintStream,
    width: Int = preferredWidth(),
    stripColor: Boolean = noColor(),
) {
    out.println(toRenderedString(width, stripColor))
    out.flush()
}

// ---- TerminalScope integrations ----

/**
 * Write [component] to this terminal scope at the given width. SGR escapes
 * are stripped when [stripColor] is true (default: `NO_COLOR`); cursor
 * movement and hyperlink escapes still flow through to the terminal.
 */
public fun TerminalScope.render(
    component: Component,
    width: Int = component.preferredWidth(),
    stripColor: Boolean = noColor(),
) {
    val lines = component.render(width)
    for ((i, line) in lines.withIndex()) {
        if (i > 0) print("\n")
        print(if (stripColor) stripSgr(line) else line)
    }
}

/** Render a [frame] inline inside `terminal { }`. */
public fun TerminalScope.frame(block: FrameScope.() -> Unit) {
    render(io.github.krossterm.tui.frame(block))
}

/** Render a [frame] with a title inline inside `terminal { }`. */
public fun TerminalScope.frame(title: String, block: FrameScope.() -> Unit) {
    render(io.github.krossterm.tui.frame(title, block))
}

/** Render a [table] inline inside `terminal { }`. */
public fun TerminalScope.table(block: TableScope.() -> Unit) {
    render(io.github.krossterm.tui.table(block))
}

/** Render a [column] inline inside `terminal { }`. */
public fun TerminalScope.column(block: ColumnScope.() -> Unit) {
    render(io.github.krossterm.tui.column(block))
}

/** Render a [row] inline inside `terminal { }`. */
public fun TerminalScope.row(block: RowScope.() -> Unit) {
    render(io.github.krossterm.tui.row(block))
}

/** Render a [divider] inline inside `terminal { }`. */
public fun TerminalScope.divider(
    title: String? = null,
    borderStyle: BorderStyle = BorderStyle.BoxDraw,
    preferredWidth: Int = 60,
) {
    render(io.github.krossterm.tui.divider(title, borderStyle, preferredWidth))
}

/** Render a [text] block inline inside `terminal { }`. */
public fun TerminalScope.text(content: String, align: Align = Align.Start) {
    render(io.github.krossterm.tui.text(content, align))
}

/** Render a [progressBar] inline inside `terminal { }`. */
public fun TerminalScope.progressBar(
    value: Number,
    total: Number = 100,
    style: ProgressStyle = ProgressStyle.Blocks,
    label: String? = null,
    showPercent: Boolean = true,
    brackets: Pair<String, String>? = "[" to "]",
    width: Int? = null,
) {
    render(
        io.github.krossterm.tui.progressBar(
            value = value,
            total = total,
            style = style,
            label = label,
            showPercent = showPercent,
            brackets = brackets,
            width = width,
        )
    )
}

/** Render a [progressBar] (DSL form) inline inside `terminal { }`. */
public fun TerminalScope.progressBar(block: ProgressBarScope.() -> Unit) {
    render(io.github.krossterm.tui.progressBar(block))
}

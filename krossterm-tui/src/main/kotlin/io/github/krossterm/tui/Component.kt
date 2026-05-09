package io.github.krossterm.tui

/**
 * A renderable TUI component. Components are immutable, immediate-mode
 * producers of styled text: each call to [render] returns the lines that
 * make up the component for the requested width.
 *
 * Every line returned by [render] is exactly [width] visible columns wide
 * after stripping ANSI escapes (right-padded with spaces if the natural
 * content is narrower). This invariant is what makes [row] and [column]
 * composition work — children can be lined up cell-by-cell without
 * tracking widths separately.
 */
public interface Component {
    /** Width this component would prefer if no constraint is given. */
    public fun preferredWidth(): Int

    /** Render at the given width. Each returned line has visible width [width]. */
    public fun render(width: Int): List<String>
}

/** Render at [preferredWidth] and join with newlines. Convenient for printing. */
public fun Component.render(): String =
    render(preferredWidth()).joinToString("\n")

/** A leaf block already rendered to lines of fixed width. */
internal class Block(
    private val width: Int,
    private val lines: List<String>,
) : Component {
    override fun preferredWidth(): Int = width
    override fun render(width: Int): List<String> {
        if (width == this.width) return lines
        return lines.map { padOrTruncate(it, width) }
    }
}

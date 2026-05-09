package io.github.krossterm.tui

/**
 * Horizontal rule. With no [title], renders as a solid line of the given
 * [borderStyle]'s horizontal glyph. With a title, renders as
 * `── Title ────────`. [preferredWidth] controls the natural rendered width
 * when no width is given by a parent — useful when the divider is the
 * top-level component being printed directly.
 */
public fun divider(
    title: String? = null,
    borderStyle: BorderStyle = BorderStyle.BoxDraw,
    preferredWidth: Int = 60,
): Component = DividerComponent(title, borderStyle, preferredWidth)

private class DividerComponent(
    private val title: String?,
    private val border: BorderStyle,
    private val preferred: Int,
) : Component {
    override fun preferredWidth(): Int = preferred.coerceAtLeast(1)

    override fun render(width: Int): List<String> {
        if (width <= 0) return emptyList()
        val title = this.title
        if (title == null) return listOf(border.horizontal.repeat(width))
        val titleText = " $title "
        val titleWidth = visibleWidth(titleText)
        if (titleWidth >= width) return listOf(padOrTruncate(titleText, width))
        val leftDashes = 2.coerceAtMost(width - titleWidth)
        val rightDashes = width - titleWidth - leftDashes
        return listOf(
            border.horizontal.repeat(leftDashes) + titleText + border.horizontal.repeat(rightDashes)
        )
    }
}

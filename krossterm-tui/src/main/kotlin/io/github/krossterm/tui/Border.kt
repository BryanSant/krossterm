package io.github.krossterm.tui

/**
 * Glyph set for box-drawing borders. Field names follow the visual position:
 * `topLeft` / `topRight` / `bottomLeft` / `bottomRight` are corners,
 * `horizontal` / `vertical` are edges, and the various T- and cross-pieces
 * are used by tables. Border-less components only need the corners and edges.
 *
 * The companion object exposes the common presets — most callers will use
 * one of these directly rather than constructing their own.
 */
public data class BorderStyle(
    public val topLeft: String,
    public val topRight: String,
    public val bottomLeft: String,
    public val bottomRight: String,
    public val horizontal: String,
    public val vertical: String,
    public val teeDown: String,
    public val teeUp: String,
    public val teeRight: String,
    public val teeLeft: String,
    public val cross: String,
) {
    public companion object {
        public val Ascii: BorderStyle = BorderStyle(
            topLeft = "+", topRight = "+", bottomLeft = "+", bottomRight = "+",
            horizontal = "-", vertical = "|",
            teeDown = "+", teeUp = "+", teeRight = "+", teeLeft = "+", cross = "+",
        )

        public val BoxDraw: BorderStyle = BorderStyle(
            topLeft = "┌", topRight = "┐", bottomLeft = "└", bottomRight = "┘",
            horizontal = "─", vertical = "│",
            teeDown = "┬", teeUp = "┴", teeRight = "├", teeLeft = "┤", cross = "┼",
        )

        public val Rounded: BorderStyle = BoxDraw.copy(
            topLeft = "╭", topRight = "╮", bottomLeft = "╰", bottomRight = "╯",
        )

        public val Double: BorderStyle = BorderStyle(
            topLeft = "╔", topRight = "╗", bottomLeft = "╚", bottomRight = "╝",
            horizontal = "═", vertical = "║",
            teeDown = "╦", teeUp = "╩", teeRight = "╠", teeLeft = "╣", cross = "╬",
        )

        public val Heavy: BorderStyle = BorderStyle(
            topLeft = "┏", topRight = "┓", bottomLeft = "┗", bottomRight = "┛",
            horizontal = "━", vertical = "┃",
            teeDown = "┳", teeUp = "┻", teeRight = "┣", teeLeft = "┫", cross = "╋",
        )
    }
}

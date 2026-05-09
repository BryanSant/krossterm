package io.github.krossterm.tui

/**
 * Builder receiver for [frame]. Configures the frame's [title], [borderStyle],
 * and [padding], and accumulates body components via `+component` or [add].
 * The body lays out as a column.
 */
@KrosstermTuiDsl
public class FrameScope @PublishedApi internal constructor() {
    public var title: String? = null
    public var borderStyle: BorderStyle = BorderStyle.Rounded
    public var padding: Int = 1

    @PublishedApi internal val children: MutableList<Component> = mutableListOf()

    public operator fun Component.unaryPlus() {
        children += this
    }

    public fun add(component: Component) {
        children += component
    }

    public fun spacer(lines: Int = 1) {
        repeat(lines) { children += BlankLine }
    }
}

/**
 * Bordered container with optional title. The body lays out as a column,
 * surrounded by a border of [BorderStyle] (default [BorderStyle.Rounded])
 * and inset by [FrameScope.padding] columns / blank lines.
 */
public inline fun frame(block: FrameScope.() -> Unit): Component {
    val scope = FrameScope()
    scope.block()
    return FrameComponent(
        title = scope.title,
        border = scope.borderStyle,
        padding = scope.padding.coerceAtLeast(0),
        body = ColumnComponent(scope.children.toList()),
    )
}

/** Convenience shorthand: `frame("Title") { + content }`. */
public inline fun frame(title: String, block: FrameScope.() -> Unit): Component =
    frame {
        this.title = title
        block()
    }

@PublishedApi
internal class FrameComponent(
    private val title: String?,
    private val border: BorderStyle,
    private val padding: Int,
    private val body: Component,
) : Component {
    override fun preferredWidth(): Int {
        val bodyWidth = body.preferredWidth() + padding * 2
        val titleWidth = title?.let { visibleWidth(it) + 4 } ?: 0
        return maxOf(bodyWidth, titleWidth) + 2
    }

    override fun render(width: Int): List<String> {
        val outerWidth = width.coerceAtLeast(2)
        val innerWidth = (outerWidth - 2 - padding * 2).coerceAtLeast(0)
        val bodyLines = body.render(innerWidth)
        val out = mutableListOf<String>()
        out += topBorder(outerWidth)
        repeat(padding) { out += paddedRow(innerWidth) }
        for (line in bodyLines) out += paddedRow(innerWidth, line)
        repeat(padding) { out += paddedRow(innerWidth) }
        out += bottomBorder(outerWidth)
        return out
    }

    private fun topBorder(outer: Int): String {
        val inner = outer - 2
        val title = this.title
        if (title == null || inner < 4) {
            return border.topLeft + border.horizontal.repeat(inner) + border.topRight
        }
        val titleText = " $title "
        val titleVisible = visibleWidth(titleText)
        val totalDashes = (inner - titleVisible).coerceAtLeast(0)
        val left = 1
        val right = (totalDashes - left).coerceAtLeast(0)
        val truncatedTitle = if (titleVisible > inner - 2) padOrTruncate(titleText, inner - 2) else titleText
        return buildString {
            append(border.topLeft)
            append(border.horizontal.repeat(left))
            append(truncatedTitle)
            append(border.horizontal.repeat(right))
            append(border.topRight)
        }
    }

    private fun bottomBorder(outer: Int): String {
        val inner = outer - 2
        return border.bottomLeft + border.horizontal.repeat(inner) + border.bottomRight
    }

    private fun paddedRow(innerWidth: Int, content: String = ""): String {
        val pad = " ".repeat(padding)
        val body = padOrTruncate(content, innerWidth)
        return border.vertical + pad + body + pad + border.vertical
    }
}

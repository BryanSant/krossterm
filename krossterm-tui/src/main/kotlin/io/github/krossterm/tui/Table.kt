package io.github.krossterm.tui

/**
 * Builder receiver for [table]. Configure headers, per-column alignment,
 * border style, and divider visibility, then add rows via [row], [rows],
 * or `+listOf(...)`.
 *
 * Cells can be raw strings (already-styled output from `krossterm.style`
 * extensions like `"hi".red().bold()` is fine — ANSI is preserved). For
 * richer cells, wrap with [text] and pass via [rowOf].
 */
@KrosstermTuiDsl
public class TableScope @PublishedApi internal constructor() {
    public var borderStyle: BorderStyle = BorderStyle.Rounded
    public var rowDivider: Boolean = false
    public var headerDivider: Boolean = true
    public var padding: Int = 1

    @PublishedApi internal var headers: List<String> = emptyList()
    @PublishedApi internal var alignments: List<Align> = emptyList()
    @PublishedApi internal val rows: MutableList<List<Component>> = mutableListOf()

    public fun headers(vararg names: String) {
        headers = names.toList()
    }

    public fun align(vararg cols: Align) {
        alignments = cols.toList()
    }

    public fun row(vararg cells: String) {
        rows += cells.map { text(it) }
    }

    public fun rows(rows: Iterable<List<String>>) {
        rows.forEach { this.rows += it.map(::text) }
    }

    /** Add a row of [Component] cells (e.g. wrapped [text] with custom alignment). */
    public fun rowOf(vararg cells: Component) {
        rows += cells.toList()
    }

    public operator fun List<String>.unaryPlus() {
        rows += map(::text)
    }
}

/**
 * A grid of rows under (optional) headers. Column widths are auto-sized to
 * the widest cell. Default border is [BorderStyle.Rounded].
 */
public inline fun table(block: TableScope.() -> Unit): Component {
    val scope = TableScope()
    scope.block()
    return TableComponent(
        headers = scope.headers,
        rows = scope.rows.toList(),
        alignments = scope.alignments,
        border = scope.borderStyle,
        rowDivider = scope.rowDivider,
        headerDivider = scope.headerDivider,
        padding = scope.padding.coerceAtLeast(0),
    )
}

@PublishedApi
internal class TableComponent(
    private val headers: List<String>,
    private val rows: List<List<Component>>,
    alignments: List<Align>,
    private val border: BorderStyle,
    private val rowDivider: Boolean,
    private val headerDivider: Boolean,
    private val padding: Int,
) : Component {
    private val columnCount: Int = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    private val alignments: List<Align> = List(columnCount) { i -> alignments.getOrElse(i) { Align.Start } }

    override fun preferredWidth(): Int {
        val cols = naturalColumnWidths()
        return cols.sum() + cols.size * padding * 2 + cols.size + 1
    }

    override fun render(width: Int): List<String> {
        if (columnCount == 0) return emptyList()
        val cols = fitColumnWidths(width)
        val out = mutableListOf<String>()

        out += borderLine(cols, BorderEdge.Top)
        if (headers.isNotEmpty()) {
            out += dataRow(cols, headerCells())
            if (headerDivider && rows.isNotEmpty()) out += borderLine(cols, BorderEdge.Mid)
        }
        for ((index, row) in rows.withIndex()) {
            out += dataRow(cols, padRowCells(row))
            if (rowDivider && index != rows.lastIndex) out += borderLine(cols, BorderEdge.Mid)
        }
        out += borderLine(cols, BorderEdge.Bottom)
        return out
    }

    private fun headerCells(): List<Component> = List(columnCount) { i ->
        text(headers.getOrElse(i) { "" }, alignments[i])
    }

    private fun padRowCells(row: List<Component>): List<Component> =
        List(columnCount) { i -> row.getOrElse(i) { BlankLine } }

    private fun naturalColumnWidths(): IntArray {
        val widths = IntArray(columnCount)
        for (i in 0 until columnCount) {
            val headerW = visibleWidth(headers.getOrElse(i) { "" })
            val rowsW = rows.maxOfOrNull { row ->
                row.getOrNull(i)?.preferredWidth() ?: 0
            } ?: 0
            widths[i] = maxOf(headerW, rowsW, 1)
        }
        return widths
    }

    private fun fitColumnWidths(targetOuter: Int): IntArray {
        val natural = naturalColumnWidths()
        val overhead = columnCount * padding * 2 + columnCount + 1
        val targetInner = (targetOuter - overhead).coerceAtLeast(columnCount)
        val naturalSum = natural.sum()
        if (naturalSum == targetInner) return natural
        val widths = natural.copyOf()
        if (naturalSum < targetInner) {
            var slack = targetInner - naturalSum
            var i = 0
            while (slack > 0) {
                widths[i % columnCount]++
                slack--
                i++
            }
            return widths
        }
        var excess = naturalSum - targetInner
        var i = 0
        while (excess > 0) {
            val idx = i % columnCount
            if (widths[idx] > 1) {
                widths[idx]--
                excess--
            } else if (widths.all { it <= 1 }) {
                break
            }
            i++
        }
        return widths
    }

    private fun dataRow(cols: IntArray, cells: List<Component>): String {
        val rendered = cells.mapIndexed { i, cell -> cell.render(cols[i]) }
        val height = rendered.maxOf { it.size }
        val padded = rendered.mapIndexed { i, lines ->
            if (lines.size == height) lines
            else lines + List(height - lines.size) { " ".repeat(cols[i]) }
        }
        return (0 until height).joinToString("\n") { row ->
            buildString {
                append(border.vertical)
                for ((i, lineSet) in padded.withIndex()) {
                    append(" ".repeat(padding))
                    append(lineSet[row])
                    append(" ".repeat(padding))
                    append(border.vertical)
                }
            }
        }
    }

    private enum class BorderEdge { Top, Mid, Bottom }

    private fun borderLine(cols: IntArray, edge: BorderEdge): String {
        val (left, mid, right) = when (edge) {
            BorderEdge.Top -> Triple(border.topLeft, border.teeDown, border.topRight)
            BorderEdge.Mid -> Triple(border.teeRight, border.cross, border.teeLeft)
            BorderEdge.Bottom -> Triple(border.bottomLeft, border.teeUp, border.bottomRight)
        }
        return buildString {
            append(left)
            for ((i, w) in cols.withIndex()) {
                append(border.horizontal.repeat(w + padding * 2))
                append(if (i == cols.lastIndex) right else mid)
            }
        }
    }
}

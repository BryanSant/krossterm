package io.github.krossterm.tui

/**
 * Builder receiver for [column]. Children stack vertically; each renders at
 * the column's full width. Use `+component` to append, or [add].
 */
@KrosstermTuiDsl
public class ColumnScope @PublishedApi internal constructor() {
    @PublishedApi internal val children: MutableList<Component> = mutableListOf()

    public operator fun Component.unaryPlus() {
        children += this
    }

    public fun add(component: Component) {
        children += component
    }

    /** Insert a blank spacer of [lines] empty rows. */
    public fun spacer(lines: Int = 1) {
        repeat(lines) { children += BlankLine }
    }
}

/**
 * Stack components vertically. Each child renders at the column's full width
 * (or [preferredWidth] if no width is given), with children's lines
 * concatenated in order.
 */
public inline fun column(block: ColumnScope.() -> Unit): Component {
    val scope = ColumnScope()
    scope.block()
    return ColumnComponent(scope.children.toList())
}

@PublishedApi
internal class ColumnComponent(
    private val children: List<Component>,
) : Component {
    override fun preferredWidth(): Int =
        children.maxOfOrNull { it.preferredWidth() } ?: 0

    override fun render(width: Int): List<String> =
        children.flatMap { it.render(width) }
}

@PublishedApi
internal object BlankLine : Component {
    override fun preferredWidth(): Int = 0
    override fun render(width: Int): List<String> = listOf(" ".repeat(width.coerceAtLeast(0)))
}

/**
 * Builder receiver for [row]. Use `+component` for an equal-weight cell, or
 * [cell] / [add] to set an explicit weight or fixed width. Heights are
 * harmonized to the tallest child; shorter children are bottom-padded with
 * blank lines of their allocated width.
 */
@KrosstermTuiDsl
public class RowScope @PublishedApi internal constructor() {
    @PublishedApi internal val children: MutableList<RowChild> = mutableListOf()

    public operator fun Component.unaryPlus() {
        children += RowChild(this, weight = 1, fixedWidth = null)
    }

    public fun add(component: Component, weight: Int = 1) {
        children += RowChild(component, weight, fixedWidth = null)
    }

    /** Add a cell with a fixed character width that's never adjusted by layout. */
    public fun fixed(width: Int, component: Component) {
        children += RowChild(component, weight = 0, fixedWidth = width)
    }

    /** Add a cell with explicit [weight] (default 1) using a builder. */
    public fun cell(weight: Int = 1, block: () -> Component) {
        children += RowChild(block(), weight, fixedWidth = null)
    }
}

@PublishedApi
internal data class RowChild(
    val component: Component,
    val weight: Int,
    val fixedWidth: Int?,
)

/**
 * Place components horizontally. Width is allocated as: fixed-width cells
 * first, then remaining width split among flex cells by weight. Each cell's
 * lines are zipped row-by-row; shorter cells are bottom-padded.
 */
public inline fun row(block: RowScope.() -> Unit): Component {
    val scope = RowScope()
    scope.block()
    return RowComponent(scope.children.toList())
}

@PublishedApi
internal class RowComponent(
    private val children: List<RowChild>,
) : Component {
    override fun preferredWidth(): Int {
        // Pick a width that, when split by weight, gives every flex child at
        // least its own preferred width. This is `max(pref/weight) * totalWeight`
        // (in ceiling arithmetic), plus the fixed-width budget.
        var fixedTotal = 0
        var totalWeight = 0
        var maxPerWeight = 0
        for (child in children) {
            if (child.fixedWidth != null) {
                fixedTotal += child.fixedWidth
                continue
            }
            val w = child.weight.coerceAtLeast(1)
            totalWeight += w
            val perWeight = (child.component.preferredWidth() + w - 1) / w
            if (perWeight > maxPerWeight) maxPerWeight = perWeight
        }
        return fixedTotal + maxPerWeight * totalWeight
    }

    override fun render(width: Int): List<String> {
        if (children.isEmpty()) return listOf(" ".repeat(width.coerceAtLeast(0)))
        val widths = allocateWidths(width)
        val rendered = children.mapIndexed { i, child -> child.component.render(widths[i]) }
        val height = rendered.maxOf { it.size }
        val padded = rendered.mapIndexed { i, lines ->
            if (lines.size == height) lines
            else lines + List(height - lines.size) { " ".repeat(widths[i]) }
        }
        return (0 until height).map { row ->
            buildString { padded.forEach { append(it[row]) } }
        }
    }

    private fun allocateWidths(total: Int): IntArray {
        val widths = IntArray(children.size)
        var remaining = total.coerceAtLeast(0)
        var totalWeight = 0
        for ((i, child) in children.withIndex()) {
            if (child.fixedWidth != null) {
                val w = child.fixedWidth.coerceAtMost(remaining)
                widths[i] = w
                remaining -= w
            } else {
                totalWeight += child.weight.coerceAtLeast(0)
            }
        }
        if (totalWeight == 0) return widths
        var distributed = 0
        var lastFlex = -1
        for ((i, child) in children.withIndex()) {
            if (child.fixedWidth != null) continue
            val share = (remaining.toLong() * child.weight / totalWeight).toInt()
            widths[i] = share
            distributed += share
            lastFlex = i
        }
        if (lastFlex >= 0 && distributed < remaining) {
            widths[lastFlex] += remaining - distributed
        }
        return widths
    }
}

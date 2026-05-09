package io.github.krossterm.tui

/**
 * Glyph set for a [progressBar]. [full] is used for completed cells, [empty]
 * for unfilled cells. [partials] is an optional list of sub-cell glyphs from
 * 1/N to (N-1)/N fill, in order. When [partials] is non-empty, the bar
 * renders fractional progress smoothly across cell boundaries.
 *
 * Use the companion presets — most callers won't construct their own.
 */
public data class ProgressStyle(
    public val full: Char,
    public val empty: Char,
    public val partials: List<Char> = emptyList(),
) {
    public companion object {
        public val Blocks: ProgressStyle = ProgressStyle(full = '█', empty = '░')
        public val Smooth: ProgressStyle = ProgressStyle(
            full = '█',
            empty = ' ',
            partials = listOf('▏', '▎', '▍', '▌', '▋', '▊', '▉'),
        )
        public val Ascii: ProgressStyle = ProgressStyle(full = '=', empty = '-')
        public val Dots: ProgressStyle = ProgressStyle(full = '⣿', empty = '⣀')
    }
}

/** Builder receiver for [progressBar]. */
@KrosstermTuiDsl
public class ProgressBarScope @PublishedApi internal constructor() {
    public var value: Number = 0
    public var total: Number = 100
    public var style: ProgressStyle = ProgressStyle.Blocks
    public var label: String? = null
    public var showPercent: Boolean = true
    public var brackets: Pair<String, String>? = "[" to "]"
    public var width: Int? = null
}

/**
 * A horizontal progress bar showing [value] out of [total] (defaulting to 0..100).
 * Renders as `[label] [bar] NN%`, with parts elided as needed when constrained.
 */
public fun progressBar(
    value: Number,
    total: Number = 100,
    style: ProgressStyle = ProgressStyle.Blocks,
    label: String? = null,
    showPercent: Boolean = true,
    brackets: Pair<String, String>? = "[" to "]",
    width: Int? = null,
): Component = ProgressBarComponent(
    value = value.toDouble(),
    total = total.toDouble(),
    style = style,
    label = label,
    showPercent = showPercent,
    brackets = brackets,
    explicitWidth = width,
)

/** DSL form: `progressBar { value = 42; total = 100; ... }`. */
public inline fun progressBar(block: ProgressBarScope.() -> Unit): Component {
    val scope = ProgressBarScope()
    scope.block()
    return progressBar(
        value = scope.value,
        total = scope.total,
        style = scope.style,
        label = scope.label,
        showPercent = scope.showPercent,
        brackets = scope.brackets,
        width = scope.width,
    )
}

private class ProgressBarComponent(
    private val value: Double,
    private val total: Double,
    private val style: ProgressStyle,
    private val label: String?,
    private val showPercent: Boolean,
    private val brackets: Pair<String, String>?,
    private val explicitWidth: Int?,
) : Component {
    private val percent: Double = if (total <= 0.0) 0.0 else (value / total).coerceIn(0.0, 1.0)

    override fun preferredWidth(): Int = explicitWidth ?: 40

    override fun render(width: Int): List<String> {
        val target = explicitWidth ?: width
        if (target <= 0) return emptyList()
        val labelStr = label?.let { "$it " } ?: ""
        val pctStr = if (showPercent) " ${(percent * 100).toInt().toString().padStart(3)}%" else ""
        val bracketL = brackets?.first ?: ""
        val bracketR = brackets?.second ?: ""
        val overhead = visibleWidth(labelStr) + visibleWidth(bracketL) + visibleWidth(bracketR) + visibleWidth(pctStr)
        val barWidth = (target - overhead).coerceAtLeast(0)
        val bar = renderBar(barWidth)
        val composed = labelStr + bracketL + bar + bracketR + pctStr
        return listOf(padOrTruncate(composed, target))
    }

    private fun renderBar(barWidth: Int): String {
        if (barWidth <= 0) return ""
        val partials = style.partials
        if (partials.isEmpty()) {
            val filled = (percent * barWidth).toInt().coerceIn(0, barWidth)
            return style.full.toString().repeat(filled) + style.empty.toString().repeat(barWidth - filled)
        }
        val buckets = partials.size + 1
        val totalSub = barWidth.toLong() * buckets
        val filledSub = (percent * totalSub).toLong().coerceIn(0, totalSub)
        val fullCells = (filledSub / buckets).toInt()
        val partialIdx = (filledSub % buckets).toInt()
        return buildString {
            repeat(fullCells) { append(style.full) }
            if (fullCells < barWidth) {
                if (partialIdx > 0) append(partials[partialIdx - 1]) else append(style.empty)
                repeat(barWidth - fullCells - 1) { append(style.empty) }
            }
        }
    }
}

package io.github.krossterm.cursor

/**
 * A cursor position: zero-indexed (column, row).
 *
 * Stored as a packed Long for allocation-free returns.
 */
@JvmInline
public value class Position internal constructor(@PublishedApi internal val packed: Long) {
    public val column: Int get() = (packed ushr 32).toInt()
    public val row: Int get() = (packed and 0xFFFFFFFFL).toInt()

    public operator fun component1(): Int = column
    public operator fun component2(): Int = row

    override fun toString(): String = "Position(column=$column, row=$row)"

    public companion object {
        public fun of(column: Int, row: Int): Position {
            require(column >= 0 && row >= 0) { "Position must be non-negative: ($column, $row)" }
            return Position((column.toLong() shl 32) or (row.toLong() and 0xFFFFFFFFL))
        }
    }
}

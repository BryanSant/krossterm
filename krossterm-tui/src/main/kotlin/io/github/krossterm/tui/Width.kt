package io.github.krossterm.tui

internal const val ESC: Char = ''
internal const val BEL: Char = ''
internal const val RESET: String = "[0m"

/**
 * Visible column count of [s], skipping ANSI CSI (`ESC [ ... letter`) and OSC
 * (`ESC ] ... BEL` or `ESC ] ... ESC \`) escape sequences. Assumes single-width
 * characters; CJK / emoji widths are not handled in this version.
 */
internal fun visibleWidth(s: String): Int {
    var width = 0
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == ESC && i + 1 < s.length && s[i + 1] == '[' -> {
                i += 2
                while (i < s.length && s[i] !in '@'..'~') i++
                if (i < s.length) i++
            }
            c == ESC && i + 1 < s.length && s[i + 1] == ']' -> {
                i += 2
                while (i < s.length) {
                    if (s[i] == BEL) { i++; break }
                    if (s[i] == ESC && i + 1 < s.length && s[i + 1] == '\\') { i += 2; break }
                    i++
                }
            }
            c == '\n' || c == '\r' -> i++
            else -> { width++; i++ }
        }
    }
    return width
}

/** Right-pad with spaces to reach [width]; if already wider, truncate (preserving styling). */
internal fun padOrTruncate(line: String, width: Int): String {
    val current = visibleWidth(line)
    return when {
        current == width -> line
        current < width -> line + " ".repeat(width - current)
        else -> truncateToVisibleWidth(line, width)
    }
}

/**
 * Truncate [line] so visible width is exactly [width]. ANSI escapes are
 * preserved verbatim. Trailing reset is appended if any styling was opened.
 */
internal fun truncateToVisibleWidth(line: String, width: Int): String {
    if (width <= 0) return ""
    val out = StringBuilder()
    var visible = 0
    var i = 0
    var styledOpened = false
    while (i < line.length && visible < width) {
        val c = line[i]
        when {
            c == ESC && i + 1 < line.length && line[i + 1] == '[' -> {
                val start = i
                i += 2
                while (i < line.length && line[i] !in '@'..'~') i++
                if (i < line.length) i++
                out.append(line, start, i)
                styledOpened = true
            }
            c == ESC && i + 1 < line.length && line[i + 1] == ']' -> {
                val start = i
                i += 2
                while (i < line.length) {
                    if (line[i] == BEL) { i++; break }
                    if (line[i] == ESC && i + 1 < line.length && line[i + 1] == '\\') { i += 2; break }
                    i++
                }
                out.append(line, start, i)
            }
            c == '\n' || c == '\r' -> i++
            else -> {
                out.append(c)
                visible++
                i++
            }
        }
    }
    if (styledOpened) out.append(RESET)
    return out.toString()
}

/**
 * Strip SGR ("Select Graphic Rendition") escape sequences only — i.e. CSI
 * sequences ending in `m`, which encode color and text attributes. Other
 * CSI sequences (cursor movement, line clearing) and OSC sequences
 * (hyperlinks, window titles) pass through untouched.
 *
 * Used at output boundaries when [noColor] is true so that NO_COLOR users
 * see plain text but the spinner's in-place cursor motion still works.
 */
internal fun stripSgr(s: String): String {
    if (s.isEmpty()) return s
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == ESC && i + 1 < s.length) {
            when (s[i + 1]) {
                '[' -> {
                    var j = i + 2
                    while (j < s.length && s[j] !in '@'..'~') j++
                    if (j >= s.length) {
                        out.append(s, i, s.length)
                        return out.toString()
                    }
                    if (s[j] == 'm') {
                        i = j + 1
                    } else {
                        out.append(s, i, j + 1)
                        i = j + 1
                    }
                    continue
                }
                ']' -> {
                    val len = ansiSeqLength(s, i)
                    out.append(s, i, i + len)
                    i += len
                    continue
                }
            }
        }
        out.append(c)
        i++
    }
    return out.toString()
}

/** True if [s] starts an ANSI CSI or OSC at index [i]. */
internal fun isAnsiStart(s: String, i: Int): Boolean =
    i + 1 < s.length && s[i] == ESC && (s[i + 1] == '[' || s[i + 1] == ']')

/** Length in chars (not visible width) of the ANSI sequence starting at [i]. */
internal fun ansiSeqLength(s: String, i: Int): Int {
    if (!isAnsiStart(s, i)) return 0
    val kind = s[i + 1]
    var j = i + 2
    if (kind == '[') {
        while (j < s.length && s[j] !in '@'..'~') j++
        if (j < s.length) j++
        return j - i
    }
    while (j < s.length) {
        if (s[j] == BEL) { j++; return j - i }
        if (s[j] == ESC && j + 1 < s.length && s[j + 1] == '\\') return j + 2 - i
        j++
    }
    return j - i
}

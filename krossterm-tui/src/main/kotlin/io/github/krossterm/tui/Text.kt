package io.github.krossterm.tui

/** Horizontal alignment within a fixed-width region. */
public enum class Align { Start, Center, End }

/**
 * A text block. Multiline input keeps its hard breaks; long lines are wrapped
 * at the rendered width. Pre-styled input from `krossterm.style` (e.g.
 * `"hi".red().bold()`) is supported transparently.
 */
public fun text(content: String, align: Align = Align.Start): Component =
    Text(content, align)

/** Convenience: `text(any.toString(), align)` for non-string values. */
public fun text(content: Any?, align: Align = Align.Start): Component =
    Text(content.toString(), align)

/**
 * A block of text. Wrapping is whitespace-based (greedy fill). Existing
 * newlines force a hard break, so banners with explicit line breaks render
 * unchanged. Pre-styled input (`"hi".red().bold()`) is fine — ANSI escapes
 * are preserved verbatim and ignored when measuring width.
 */
public class Text internal constructor(
    private val content: String,
    private val align: Align,
) : Component {
    override fun preferredWidth(): Int =
        content.lineSequence().maxOf { visibleWidth(it) }

    override fun render(width: Int): List<String> {
        if (width <= 0) return emptyList()
        val out = mutableListOf<String>()
        for (paragraph in content.lineSequence()) {
            if (visibleWidth(paragraph) <= width) {
                out += alignLine(paragraph, width)
            } else {
                out += wrap(paragraph, width).map { alignLine(it, width) }
            }
        }
        return out
    }

    private fun alignLine(line: String, width: Int): String {
        val w = visibleWidth(line)
        if (w >= width) return padOrTruncate(line, width)
        val pad = width - w
        return when (align) {
            Align.Start -> line + " ".repeat(pad)
            Align.End -> " ".repeat(pad) + line
            Align.Center -> {
                val left = pad / 2
                val right = pad - left
                " ".repeat(left) + line + " ".repeat(right)
            }
        }
    }
}

/**
 * Greedy whitespace wrap. Splits on space runs; tokens that exceed [width]
 * are hard-broken at column boundaries. ANSI escapes inside a token are
 * preserved verbatim.
 */
internal fun wrap(line: String, width: Int): List<String> {
    if (width <= 0) return emptyList()
    val tokens = splitOnSpaces(line)
    val lines = mutableListOf<StringBuilder>()
    var current = StringBuilder()
    var currentWidth = 0
    for (tok in tokens) {
        val tokWidth = visibleWidth(tok.text)
        if (tok.isSpace) {
            if (currentWidth == 0) continue
            if (currentWidth + tokWidth > width) {
                lines += current
                current = StringBuilder()
                currentWidth = 0
                continue
            }
            current.append(tok.text)
            currentWidth += tokWidth
            continue
        }
        if (tokWidth > width) {
            if (currentWidth > 0) {
                lines += current
                current = StringBuilder()
                currentWidth = 0
            }
            for (chunk in hardBreak(tok.text, width)) {
                lines += StringBuilder(chunk)
            }
            current = lines.removeAt(lines.size - 1)
            currentWidth = visibleWidth(current.toString())
            continue
        }
        if (currentWidth + tokWidth > width) {
            lines += current
            current = StringBuilder()
            currentWidth = 0
        }
        current.append(tok.text)
        currentWidth += tokWidth
    }
    if (currentWidth > 0) lines += current
    return lines.map { it.toString().trimEnd() }
}

private data class Token(val text: String, val isSpace: Boolean)

private fun splitOnSpaces(line: String): List<Token> {
    val out = mutableListOf<Token>()
    val buf = StringBuilder()
    val pendingAnsi = StringBuilder()
    var inSpace: Boolean? = null
    var i = 0
    while (i < line.length) {
        if (isAnsiStart(line, i)) {
            val len = ansiSeqLength(line, i)
            pendingAnsi.append(line, i, i + len)
            i += len
            continue
        }
        val c = line[i]
        val isSpace = c == ' ' || c == '\t'
        when {
            inSpace == null -> {
                inSpace = isSpace
                buf.append(pendingAnsi)
                pendingAnsi.clear()
                buf.append(c)
            }
            isSpace == inSpace -> {
                buf.append(pendingAnsi)
                pendingAnsi.clear()
                buf.append(c)
            }
            else -> {
                out += Token(buf.toString(), inSpace)
                buf.clear()
                buf.append(pendingAnsi)
                pendingAnsi.clear()
                buf.append(c)
                inSpace = isSpace
            }
        }
        i++
    }
    if (buf.isNotEmpty() || pendingAnsi.isNotEmpty()) {
        out += Token(buf.toString() + pendingAnsi.toString(), inSpace ?: false)
    }
    return out
}

private fun hardBreak(text: String, width: Int): List<String> {
    val out = mutableListOf<String>()
    val buf = StringBuilder()
    var visible = 0
    var i = 0
    while (i < text.length) {
        if (isAnsiStart(text, i)) {
            val len = ansiSeqLength(text, i)
            buf.append(text, i, i + len)
            i += len
            continue
        }
        buf.append(text[i])
        visible++
        i++
        if (visible == width) {
            out += buf.toString()
            buf.clear()
            visible = 0
        }
    }
    if (buf.isNotEmpty()) out += buf.toString()
    return out
}

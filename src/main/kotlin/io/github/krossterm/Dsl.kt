package io.github.krossterm

import io.github.krossterm.cursor.Hide
import io.github.krossterm.cursor.MoveDown
import io.github.krossterm.cursor.MoveLeft
import io.github.krossterm.cursor.MoveRight
import io.github.krossterm.cursor.MoveTo
import io.github.krossterm.cursor.MoveToColumn
import io.github.krossterm.cursor.MoveToNextLine
import io.github.krossterm.cursor.MoveToPreviousLine
import io.github.krossterm.cursor.MoveToRow
import io.github.krossterm.cursor.MoveUp
import io.github.krossterm.cursor.RestorePosition
import io.github.krossterm.cursor.SavePosition
import io.github.krossterm.cursor.Show
import io.github.krossterm.style.Attribute
import io.github.krossterm.style.Attributes
import io.github.krossterm.style.Color
import io.github.krossterm.style.ContentStyle
import io.github.krossterm.style.Print
import io.github.krossterm.style.ResetColor
import io.github.krossterm.style.SetAttribute
import io.github.krossterm.style.SetAttributes
import io.github.krossterm.style.SetBackgroundColor
import io.github.krossterm.style.SetForegroundColor
import io.github.krossterm.style.SetStyle
import io.github.krossterm.style.SetUnderlineColor
import io.github.krossterm.terminal.Clear
import io.github.krossterm.terminal.ClearType
import io.github.krossterm.terminal.DisableLineWrap
import io.github.krossterm.terminal.EnableLineWrap
import io.github.krossterm.terminal.EnterAlternateScreen
import io.github.krossterm.terminal.LeaveAlternateScreen
import io.github.krossterm.terminal.ScrollDown
import io.github.krossterm.terminal.ScrollUp
import io.github.krossterm.terminal.SetSize
import io.github.krossterm.terminal.SetTitle
import io.github.krossterm.notification.SendNotification
import io.github.krossterm.progress.ProgressState
import io.github.krossterm.progress.SetProgress
import java.io.Writer

/** Restricts implicit receivers so `terminal { terminal { … } }` doesn't compile. */
@DslMarker
public annotation class KrosstermDsl

/**
 * Receiver for the [terminal] DSL. Every Command has a same-named lower-camel
 * shorthand here. For commands not yet shorthanded, use [command] or `+command`.
 */
@KrosstermDsl
public class TerminalScope @PublishedApi internal constructor(
    @PublishedApi internal val out: Writer,
) {
    // ---- Cursor ----
    public fun moveTo(column: Int, row: Int): Unit = MoveTo(column, row).writeAnsi(out)
    public fun moveToColumn(column: Int): Unit = MoveToColumn(column).writeAnsi(out)
    public fun moveToRow(row: Int): Unit = MoveToRow(row).writeAnsi(out)
    public fun moveToNextLine(n: Int = 1): Unit = MoveToNextLine(n).writeAnsi(out)
    public fun moveToPreviousLine(n: Int = 1): Unit = MoveToPreviousLine(n).writeAnsi(out)
    public fun moveUp(n: Int = 1): Unit = MoveUp(n).writeAnsi(out)
    public fun moveDown(n: Int = 1): Unit = MoveDown(n).writeAnsi(out)
    public fun moveLeft(n: Int = 1): Unit = MoveLeft(n).writeAnsi(out)
    public fun moveRight(n: Int = 1): Unit = MoveRight(n).writeAnsi(out)
    public fun savePosition(): Unit = SavePosition.writeAnsi(out)
    public fun restorePosition(): Unit = RestorePosition.writeAnsi(out)
    public fun hide(): Unit = Hide.writeAnsi(out)
    public fun show(): Unit = Show.writeAnsi(out)

    // ---- Terminal ----
    public fun clear(type: ClearType = ClearType.All): Unit = Clear(type).writeAnsi(out)
    public fun scrollUp(n: Int = 1): Unit = ScrollUp(n).writeAnsi(out)
    public fun scrollDown(n: Int = 1): Unit = ScrollDown(n).writeAnsi(out)
    public fun setSize(columns: Int, rows: Int): Unit = SetSize(columns, rows).writeAnsi(out)
    public fun setTitle(title: String): Unit = SetTitle(title).writeAnsi(out)
    public fun setProgress(state: ProgressState): Unit = SetProgress(state).writeAnsi(out)
    public fun sendNotification(title: String, body: String): Unit = SendNotification(title, body).writeAnsi(out)
    public fun enterAlternateScreen(): Unit = EnterAlternateScreen.writeAnsi(out)
    public fun leaveAlternateScreen(): Unit = LeaveAlternateScreen.writeAnsi(out)
    public fun enableLineWrap(): Unit = EnableLineWrap.writeAnsi(out)
    public fun disableLineWrap(): Unit = DisableLineWrap.writeAnsi(out)

    // ---- Style ----
    public fun setForegroundColor(color: Color): Unit = SetForegroundColor(color).writeAnsi(out)
    public fun setBackgroundColor(color: Color): Unit = SetBackgroundColor(color).writeAnsi(out)
    public fun setUnderlineColor(color: Color): Unit = SetUnderlineColor(color).writeAnsi(out)
    public fun setAttribute(attribute: Attribute): Unit = SetAttribute(attribute).writeAnsi(out)
    public fun setAttributes(attributes: Attributes): Unit = SetAttributes(attributes).writeAnsi(out)
    public fun setStyle(style: ContentStyle): Unit = SetStyle(style).writeAnsi(out)
    public fun resetColor(): Unit = ResetColor.writeAnsi(out)

    // ---- Print ----
    public fun print(value: Any?): Unit = Print(value).writeAnsi(out)
    public fun println(value: Any? = ""): Unit { Print(value).writeAnsi(out); out.append('\n') }

    // ---- Escape hatches ----
    public operator fun Command.unaryPlus(): Unit = writeAnsi(out)
    public fun command(c: Command): Unit = c.writeAnsi(out)
    public fun raw(s: String): Unit { out.append(s) }
}

/**
 * Open a [TerminalScope] over this writer. The block is executed against the
 * scope receiver; the writer is flushed on exit (including via exception).
 */
public inline fun <T> Writer.terminal(block: TerminalScope.() -> T): T {
    val scope = TerminalScope(this)
    return try {
        scope.block()
    } finally {
        flush()
    }
}

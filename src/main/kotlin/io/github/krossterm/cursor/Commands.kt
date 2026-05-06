package io.github.krossterm.cursor

import io.github.krossterm.Command
import io.github.krossterm.ansi.Csi.CSI
import io.github.krossterm.ansi.Csi.ESC

// ---- Movement ----

/** Move to absolute (column, row). Both are 0-indexed; ANSI uses 1-indexed and we adjust. */
public data class MoveTo(val column: Int, val row: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((row + 1).toString()).append(';').append((column + 1).toString()).append('H')
    }
}

public data class MoveToColumn(val column: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((column + 1).toString()).append('G')
    }
}

public data class MoveToRow(val row: Int) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append((row + 1).toString()).append('d')
    }
}

public data class MoveToNextLine(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('E')
    }
}

public data class MoveToPreviousLine(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('F')
    }
}

public data class MoveUp(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('A')
    }
}

public data class MoveDown(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('B')
    }
}

public data class MoveRight(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('C')
    }
}

public data class MoveLeft(val n: Int = 1) : Command {
    override fun writeAnsi(out: Appendable) {
        if (n != 0) out.append(CSI).append(n.toString()).append('D')
    }
}

// ---- Save/restore (DECSC / DECRC) ----

public data object SavePosition : Command {
    override fun writeAnsi(out: Appendable) { out.append(ESC).append('7') }
}

public data object RestorePosition : Command {
    override fun writeAnsi(out: Appendable) { out.append(ESC).append('8') }
}

// ---- Visibility ----

public data object Hide : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?25l") }
}

public data object Show : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?25h") }
}

// ---- Blinking ----

public data object EnableBlinking : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?12h") }
}

public data object DisableBlinking : Command {
    override fun writeAnsi(out: Appendable) { out.append(CSI).append("?12l") }
}

// ---- Shape ----

public data class SetCursorStyle(val style: CursorStyle) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append(style.code.toString()).append(" q")
    }
}

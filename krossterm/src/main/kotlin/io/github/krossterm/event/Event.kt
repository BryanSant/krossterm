package io.github.krossterm.event

/** A terminal event delivered by [io.github.krossterm.Terminal.events]. */
public sealed interface Event {

    /** Terminal window gained focus (CSI I). */
    public data object FocusGained : Event

    /** Terminal window lost focus (CSI O). */
    public data object FocusLost : Event

    /** A keyboard event. */
    public data class Key(val event: KeyEvent) : Event {
        public constructor(code: KeyCode, modifiers: KeyModifiers = KeyModifiers.NONE) :
            this(KeyEvent(code, modifiers))
    }

    /** A mouse event. */
    public data class Mouse(val event: MouseEvent) : Event

    /** A bracketed-paste payload (between CSI 200~ and CSI 201~). */
    public data class Paste(val text: String) : Event

    /** Terminal resized; new dimensions in cells (columns, rows). */
    public data class Resize(val columns: Int, val rows: Int) : Event

    public val isKeyPress: Boolean
        get() = this is Key && event.kind == KeyEventKind.Press

    public val isKeyRelease: Boolean
        get() = this is Key && event.kind == KeyEventKind.Release

    public val isKeyRepeat: Boolean
        get() = this is Key && event.kind == KeyEventKind.Repeat
}

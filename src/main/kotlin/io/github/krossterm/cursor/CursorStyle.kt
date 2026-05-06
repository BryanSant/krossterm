package io.github.krossterm.cursor

/**
 * Cursor shape variants, mapped to the DECSCUSR control codes (`CSI N q`).
 */
public enum class CursorStyle(internal val code: Int) {
    DefaultUserShape(0),
    BlinkingBlock(1),
    SteadyBlock(2),
    BlinkingUnderscore(3),
    SteadyUnderscore(4),
    BlinkingBar(5),
    SteadyBar(6),
}

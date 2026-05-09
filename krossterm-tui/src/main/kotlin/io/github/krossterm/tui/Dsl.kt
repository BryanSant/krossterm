package io.github.krossterm.tui

/**
 * Restricts implicit receivers in TUI builders so e.g.
 * `frame { frame { … } }` doesn't accidentally call the outer scope.
 * Use the explicit top-level [frame] / [table] / [row] / [column] functions
 * for nested components.
 */
@DslMarker
public annotation class KrosstermTuiDsl

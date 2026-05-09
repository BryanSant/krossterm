package io.github.krossterm.tui

import io.github.krossterm.terminal
import io.github.krossterm.terminal.ClearType
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Writer

/**
 * Frames and timing for a [Spinner]. The spinner cycles through [frames]
 * once every [intervalMs] milliseconds. Use the companion presets unless
 * you really want a custom cadence.
 */
public data class SpinnerStyle(
    public val frames: List<String>,
    public val intervalMs: Long,
) {
    public companion object {
        public val Dots: SpinnerStyle = SpinnerStyle(
            frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"),
            intervalMs = 80,
        )
        public val Line: SpinnerStyle = SpinnerStyle(
            frames = listOf("-", "\\", "|", "/"),
            intervalMs = 100,
        )
        public val Arrow: SpinnerStyle = SpinnerStyle(
            frames = listOf("←", "↖", "↑", "↗", "→", "↘", "↓", "↙"),
            intervalMs = 120,
        )
        public val Bounce: SpinnerStyle = SpinnerStyle(
            frames = listOf("⠁", "⠂", "⠄", "⠂"),
            intervalMs = 120,
        )
        public val Pulse: SpinnerStyle = SpinnerStyle(
            frames = listOf("●", "◐", "◑", "◒", "◓", "◔", "◕", "◖", "◗"),
            intervalMs = 100,
        )
    }
}

/**
 * Mutable handle exposed inside [withSpinner]. Update [label] mid-run to
 * reflect progress; the next frame will pick up the change.
 */
public class Spinner internal constructor(
    initialLabel: String,
    internal val style: SpinnerStyle,
) {
    @Volatile public var label: String = initialLabel

    @Volatile internal var frameIndex: Int = 0

    internal fun currentFrame(): String =
        "${style.frames[frameIndex % style.frames.size]} $label"

    internal fun advance() {
        frameIndex = (frameIndex + 1) % style.frames.size
    }
}

/**
 * Run [block] with a spinner animating on [out]. The block receives a
 * [Spinner] handle whose [Spinner.label] can be updated mid-run to reflect
 * progress (e.g. `spinner.label = "Almost there…"`).
 *
 * Behavior depends on whether the output is a TTY:
 * - **Interactive** ([interactive] == true): the spinner animates in place,
 *   the cursor is hidden for the duration, and the line is cleared on exit.
 * - **Non-interactive** (piped, redirected): no animation. The initial label
 *   is printed on a fresh line, and each subsequent label change is also
 *   emitted on its own line — so a sequence of `spinner.label = "…"`
 *   produces a readable log instead of garbled escape codes.
 *
 * [interactive] defaults to auto-detect via [isInteractive].
 */
public suspend fun <T> withSpinner(
    label: String,
    style: SpinnerStyle = SpinnerStyle.Dots,
    out: Writer = System.out.writer(),
    interactive: Boolean = isInteractive(),
    stripColor: Boolean = noColor(),
    block: suspend (Spinner) -> T,
): T {
    val spinner = Spinner(label, style)
    return coroutineScope {
        val worker = if (interactive) launchAnimator(out, spinner, stripColor)
                     else launchLabelLogger(out, spinner, stripColor)
        try {
            block(spinner)
        } finally {
            worker.cancelAndJoin()
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchAnimator(
    out: Writer,
    spinner: Spinner,
    stripColor: Boolean,
) = launch {
    out.terminal { hide() }
    try {
        while (isActive) {
            val frame = spinner.currentFrame()
            val rendered = if (stripColor) stripSgr(frame) else frame
            out.terminal {
                moveToColumn(0)
                clear(ClearType.UntilNewLine)
                print(rendered)
            }
            delay(spinner.style.intervalMs)
            spinner.advance()
        }
    } finally {
        out.terminal {
            moveToColumn(0)
            clear(ClearType.UntilNewLine)
            show()
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchLabelLogger(
    out: Writer,
    spinner: Spinner,
    stripColor: Boolean,
) = launch {
    fun render(s: String) = if (stripColor) stripSgr(s) else s
    var lastLabel = spinner.label
    out.append(render(lastLabel)).append('\n')
    out.flush()
    while (isActive) {
        delay(50)
        val current = spinner.label
        if (current != lastLabel) {
            out.append(render(current)).append('\n')
            out.flush()
            lastLabel = current
        }
    }
}

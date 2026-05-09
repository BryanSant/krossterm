package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.Hide
import io.github.krossterm.MoveTo
import io.github.krossterm.Show
import io.github.krossterm.event.DisableMouseCapture
import io.github.krossterm.event.EnableMouseCapture
import io.github.krossterm.event.Event
import io.github.krossterm.event.KeyCode
import io.github.krossterm.event.MouseEventKind
import io.github.krossterm.execute
import io.github.krossterm.style.Color
import io.github.krossterm.style.Print
import io.github.krossterm.style.bold
import io.github.krossterm.style.green
import io.github.krossterm.style.with
import io.github.krossterm.style.yellow
import io.github.krossterm.terminal.Clear
import io.github.krossterm.terminal.ClearType
import kotlinx.coroutines.runBlocking

/**
 * Alternate-screen demo: enables mouse capture, draws a header, and prints
 * incoming events at the cursor. Esc to exit.
 *
 * Run with: `./gradlew runExample -Pexample=InteractiveDemo`
 */
fun main() {
    Terminal.system().use { t ->
        t.alternateScreen {
            t.rawMode {
                // Shutdown hook so mouse capture is always disabled even on kill/crash.
                val hook = Thread {
                    runCatching { t.out.execute(DisableMouseCapture, Show) }
                }
                Runtime.getRuntime().addShutdownHook(hook)
                try {
                    t.out.execute(EnableMouseCapture, Hide, Clear(ClearType.All), MoveTo(0, 0))
                    t.synchronizedUpdate {
                        t.out.execute(
                            Print("krossterm interactive demo".bold().with(Color.Yellow)),
                            Print("\n"),
                            Print("press keys, click, scroll — Esc to quit".green()),
                            Print("\n\n"),
                        )
                    }

                    runBlocking {
                        var row = 3
                        while (true) {
                            val ev = t.readEvent()
                            if (ev is Event.Key && ev.event.code == KeyCode.Esc) break
                            val msg = when (ev) {
                                is Event.Key -> "key: ${ev.event.code} (${ev.event.modifiers})"
                                is Event.Mouse -> "mouse: ${ev.event.kind} @ (${ev.event.column}, ${ev.event.row})"
                                is Event.Resize -> "resize: ${ev.columns}x${ev.rows}"
                                is Event.Paste -> "paste: ${ev.text.take(40)}"
                                Event.FocusGained -> "focus gained"
                                Event.FocusLost -> "focus lost"
                            }
                            t.out.execute(MoveTo(0, row), Clear(ClearType.UntilNewLine), Print(msg.yellow()))
                            row += 1
                            if (row >= t.size.rows - 1) {
                                t.out.execute(Clear(ClearType.All), MoveTo(0, 3))
                                row = 3
                            }
                        }
                    }
                } finally {
                    t.out.execute(DisableMouseCapture, Show)
                    runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
                }
            }
        }
    }
}

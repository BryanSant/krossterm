package io.github.krossterm.examples

import io.github.krossterm.Terminal
import io.github.krossterm.TerminalScope
import io.github.krossterm.clipboard.CopyToClipboard
import io.github.krossterm.cursor.CursorStyle
import io.github.krossterm.cursor.Hide
import io.github.krossterm.cursor.SetCursorStyle
import io.github.krossterm.cursor.Show
import io.github.krossterm.event.DisableFocusChange
import io.github.krossterm.event.DisableMouseCapture
import io.github.krossterm.event.EnableFocusChange
import io.github.krossterm.event.EnableMouseCapture
import io.github.krossterm.event.Event
import io.github.krossterm.event.KeyCode
import io.github.krossterm.execute
import io.github.krossterm.link.EndLink
import io.github.krossterm.link.StartLink
import io.github.krossterm.progress.ClearProgress
import io.github.krossterm.progress.ProgressState
import io.github.krossterm.progress.SetProgress
import io.github.krossterm.style.Attribute
import io.github.krossterm.style.Attributes
import io.github.krossterm.style.Color
import io.github.krossterm.style.ContentStyle
import io.github.krossterm.style.bold
import io.github.krossterm.style.crossedOut
import io.github.krossterm.style.cyan
import io.github.krossterm.style.darkGrey
import io.github.krossterm.style.dim
import io.github.krossterm.style.doubleUnderlined
import io.github.krossterm.style.encircled
import io.github.krossterm.style.framed
import io.github.krossterm.style.green
import io.github.krossterm.style.italic
import io.github.krossterm.style.magenta
import io.github.krossterm.style.on
import io.github.krossterm.style.overLined
import io.github.krossterm.style.red
import io.github.krossterm.style.reverse
import io.github.krossterm.style.slowBlink
import io.github.krossterm.style.underlined
import io.github.krossterm.style.white
import io.github.krossterm.style.with
import io.github.krossterm.style.yellow
import io.github.krossterm.terminal
import kotlinx.coroutines.runBlocking

/**
 * DslShowcase — every krossterm DSL feature in one runnable example.
 *
 * Features demonstrated:
 *  - terminal { } DSL builder and the TerminalScope receiver
 *  - All 16 named colors (fg + bg), RGB truecolor, 256-color palette, hex strings
 *  - Text attributes: bold, italic, underline, dim, blink, crossedOut, overLined…
 *  - Underline colors via setUnderlineColor()
 *  - ContentStyle: fg + bg + underline + Attributes bitset, all in one setStyle() call
 *  - setStyle(), setAttributes(), setForegroundColor/BackgroundColor(), resetColor()
 *  - Cursor movement: moveTo, relative moves, moveToNextLine, save/restore, hide/show
 *  - SetCursorStyle: all six cursor shape variants (via the + operator)
 *  - Terminal commands: clear (all ClearType variants), scroll, title, line wrap
 *  - alternateScreen { }, rawMode { }, synchronizedUpdate { }
 *  - OSC 8  links  (StartLink / EndLink via + operator)
 *  - OSC 52 clipboard   (CopyToClipboard via command())
 *  - OSC 9;4 progress   (ProgressState.Normal / Error / Paused / Indeterminate)
 *  - OSC 9/99/777 desktop notification (sendNotification DSL shorthand)
 *  - Event stream: key, mouse, resize, focus, paste (interactive at the end)
 *  - Escape hatches: +Command, command(c), raw(str), t.out.execute(...)
 *
 * Run with: ./gradlew runExample -Pexample=DslShowcase
 */
fun main() {
    Terminal.system().use { t ->
        // setTitle is a DSL shorthand for SetTitle(str).writeAnsi(out).
        // Call it before alternateScreen so the title persists in the tab.
        t.out.terminal { setTitle("krossterm DSL showcase") }

        // alternateScreen { } saves the current screen, switches to a clean buffer,
        // and restores everything on exit — even if an exception is thrown.
        t.alternateScreen {

            // rawMode { } disables echo and line-buffering so every keypress is
            // delivered immediately as an Event, without waiting for Enter.
            t.rawMode {

                val hook = Thread { runCatching { t.out.execute(DisableMouseCapture, Show) } }
                Runtime.getRuntime().addShutdownHook(hook)

                try {
                    // Enable mouse and focus events so the live event stream is rich.
                    t.out.execute(EnableMouseCapture, EnableFocusChange, Hide)

                    // OSC 9;4: show a "loading" progress indicator in the terminal tab or
                    // taskbar while we prepare the showcase.  Supported by Ghostty, Ptyxis,
                    // WezTerm, Kitty, Alacritty, iTerm2, Windows Terminal, and others.
                    for (pct in 0..100 step 25) {
                        t.out.execute(SetProgress(ProgressState.Normal(pct)))
                        Thread.sleep(120)
                    }
                    t.out.execute(ClearProgress)

                    // synchronizedUpdate { } wraps the entire first render so the terminal
                    // presents it atomically — no partial-draw flicker on slow connections.
                    t.synchronizedUpdate {
                        // t.out.terminal { } is the DSL entry point.  Everything inside the
                        // block runs on a TerminalScope receiver that exposes every command
                        // as a named function.  The Writer is flushed automatically on exit.
                        t.out.terminal {
                            clear()       // clear(ClearType.All) — wipe the screen
                            moveTo(0, 0)  // absolute cursor positioning: column 0, row 0

                            printHeader(t.size.columns, t.size.rows)
                            section("COLORS — 16 named · RGB truecolor · 256-color palette · hex strings")
                            printColors()
                            section("ATTRIBUTES")
                            printAttributes()
                            section("CONTENTSTYLE — fg + bg + underlineColor + Attributes bitset")
                            printContentStyle()
                            section("CURSOR & TERMINAL COMMANDS")
                            printCursorAndTerminal()
                            section("OSC FEATURES — links · clipboard · progress · notifications")
                            printOscFeatures()
                            section("ESCAPE HATCHES — when the DSL has no shorthand")
                            printEscapeHatches()
                            section("EVENT STREAM — press keys · click · scroll   (Esc to quit)")
                        }
                    }

                    // ── Interactive event loop ───────────────────────────────────────────
                    // t.readEvent() suspends until the next Event arrives.
                    // t.events() returns a hot Flow<Event> for streaming via collect/takeWhile.
                    // t.pollEvent(timeout) returns null on timeout — useful for heartbeats.
                    runBlocking {
                        while (true) {
                            val ev = t.readEvent()
                            if (ev is Event.Key && ev.event.code == KeyCode.Esc) break

                            val line = when (ev) {
                                is Event.Key -> {
                                    val mods = if (ev.event.modifiers.isEmpty()) ""
                                               else " +${ev.event.modifiers}"
                                    "  key    ${ev.event.code}$mods  [${ev.event.kind}]".yellow()
                                }
                                is Event.Mouse ->
                                    "  mouse  ${ev.event.kind}  col=${ev.event.column} row=${ev.event.row}".cyan()
                                is Event.Resize ->
                                    "  resize ${ev.columns}×${ev.rows}".magenta()
                                is Event.Paste ->
                                    "  paste  \"${ev.text.take(50)}\"".green()
                                Event.FocusGained -> "  focus gained".white()
                                Event.FocusLost   -> "  focus lost".darkGrey()
                            }
                            // Each iteration opens a new terminal { } scope just to println.
                            // That is fine — terminal { } is a cheap inline call that flushes.
                            t.out.terminal { println(line) }
                        }
                    }

                } finally {
                    t.out.execute(DisableMouseCapture, DisableFocusChange, Show, ClearProgress)
                    runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section helpers — private TerminalScope extension functions.
// Because they run inside a terminal { } block they have full DSL access
// (moveTo, print, setStyle, +Command, …) with no extra parameters.
// ─────────────────────────────────────────────────────────────────────────────

private fun TerminalScope.section(title: String) {
    resetColor()  // clear any leftover color from the previous section
    println("── $title ".padEnd(78, '─').darkGrey())
}

private fun TerminalScope.printHeader(cols: Int, rows: Int) {
    print("  ")
    print("krossterm DSL showcase".bold().cyan())
    print("   t.out.terminal { … }".darkGrey())
    println("   [${cols}×${rows}]".darkGrey())
    println()
}

private fun TerminalScope.printColors() {
    val blk = "█"

    // ── Foreground: String.with(Color) and named extension shortcuts ──────────
    // .with(color) sets the foreground.  Named shortcuts like .red() are aliases.
    print("  fg  ")
    listOf(
        blk to Color.Black,  blk to Color.DarkRed,     blk to Color.DarkGreen,
        blk to Color.DarkYellow, blk to Color.DarkBlue, blk to Color.DarkMagenta,
        blk to Color.DarkCyan,   blk to Color.Grey,
    ).forEach { (s, c) -> print("$s ".with(c)) }
    println()
    print("       ")
    listOf(
        blk to Color.DarkGrey, blk to Color.Red,     blk to Color.Green,
        blk to Color.Yellow,   blk to Color.Blue,    blk to Color.Magenta,
        blk to Color.Cyan,     blk to Color.White,
    ).forEach { (s, c) -> print("$s ".with(c)) }
    println()

    // ── Background: String.on(Color) and named .onXxx() shortcuts ────────────
    print("  bg  ")
    listOf(
        Color.Black, Color.DarkRed,  Color.DarkGreen, Color.DarkYellow,
        Color.DarkBlue, Color.DarkMagenta, Color.DarkCyan, Color.Grey,
        Color.DarkGrey, Color.Red, Color.Green, Color.Yellow,
        Color.Blue, Color.Magenta, Color.Cyan, Color.White,
    ).forEach { c -> print("  ".on(c)) }
    println()

    // ── RGB truecolor: Color.Rgb(r, g, b) ────────────────────────────────────
    print("  RGB ")
    val w = 72
    repeat(w) { i ->
        val r = (i * 255 / w)
        val g = ((w - i) * 255 / w)
        val b = 200 - i * 150 / w
        print(blk.with(Color.Rgb(r, g, b)))
    }
    println()

    // ── 256-color palette: Color.AnsiValue(n), n in 0..255 ───────────────────
    print("  256 ")
    for (n in 0 until 16) { print(blk.with(Color.AnsiValue(n))) }
    print("  ")
    for (n in 196 until 232 step 2) { print(blk.with(Color.AnsiValue(n))) }
    print("  ")
    for (n in 232..255) { print(blk.with(Color.AnsiValue(n))) }
    println()

    // ── Hex strings: Color.parse("#rrggbb") and Color.parse("#rgb") ──────────
    // parse() returns Color? — null for unrecognised input.
    // The 3-digit form expands each nibble: #f80 → Rgb(255, 136, 0).
    print("  hex ")
    val hexSamples = listOf("#f00", "#f80", "#ff0", "#0f0", "#0ff", "#00f", "#80f", "#f0f",
                            "#c00", "#c60", "#cc0", "#0c0", "#0cc", "#00c", "#60c", "#c0c")
    hexSamples.forEach { hex -> print(blk.with(Color.parse(hex)!!)) }
    print("  ")
    listOf("#e63946", "#457b9d", "#2a9d8f", "#e9c46a", "#f4a261", "#264653").forEach { hex ->
        print("  $hex  ".with(Color.parse(hex)!!))
    }
    println()
}

private fun TerminalScope.printAttributes() {
    // Named .bold(), .italic(), etc. extension functions on String (and StyledContent).
    // They produce a StyledContent whose toString() wraps the text in SGR open + SGR reset.
    print("  ")
    print("bold ".bold())
    print("dim ".dim())
    print("italic ".italic())
    print("underlined ".underlined())
    print("double-underlined ".doubleUnderlined())
    print("slowBlink ".slowBlink())
    print("crossedOut ".crossedOut())
    println("overLined ".overLined())

    print("  ")
    print("framed ".framed())
    print("encircled ".encircled())
    print("reverse ".reverse())
    // Attributes compose: chain calls accumulate into the same ContentStyle
    print("bold+italic ".bold().italic())
    print("bold+italic+underlined ".bold().italic().underlined())
    println()

    // ── Underline colors: setUnderlineColor(Color) ────────────────────────────
    // Unlike the Stylize extensions, setUnderlineColor() is a stateful write —
    // call resetColor() (SGR 0) when done to avoid bleeding into the next line.
    print("  underline color  ")
    setUnderlineColor(Color.Red);    setAttribute(Attribute.Underlined); print("red  ")
    setUnderlineColor(Color.Green);  print("green  ")
    setUnderlineColor(Color.Blue);   print("blue")
    resetColor()
    println()
}

private fun TerminalScope.printContentStyle() {
    // setStyle(ContentStyle) writes fg + bg + underlineColor + attributes atomically
    // in a single SGR sequence.  Equivalent to calling each set* separately,
    // but cheaper and avoids intermediate resets.
    val heroStyle = ContentStyle(
        foreground  = Color.White,
        background  = Color.DarkBlue,
        underline   = Color.Cyan,
        attributes  = Attributes.of(Attribute.Bold, Attribute.Italic),
    )
    print("  setStyle(fg=White bg=DarkBlue underline=Cyan Bold+Italic) → ")
    setStyle(heroStyle)
    print("  hello, styled world!  ")
    resetColor()
    println()

    // setAttributes(Attributes) writes a bitset of attributes in one call —
    // more efficient than calling setAttribute() for each one individually.
    val multi = Attributes.of(Attribute.Bold, Attribute.Underlined, Attribute.Italic)
    print("  setAttributes(Bold + Underlined + Italic)                 → ")
    setAttributes(multi)
    print("multi-attribute bitset")
    resetColor()
    println()
}

private fun TerminalScope.printCursorAndTerminal() {
    // Cursor movement — these are all TerminalScope DSL functions.
    // moveTo() is used throughout this file to build the UI layout.
    println("  moveTo(col, row)  moveToColumn(n)  moveToRow(n)  moveToNextLine(n)  moveToPreviousLine(n)")
    println("  moveUp(n)  moveDown(n)  moveLeft(n)  moveRight(n)  savePosition()  restorePosition()")
    println("  hide()  show()   ← cursor is currently hidden; Show is called in the finally block")

    // SetCursorStyle is not in the DSL shorthands, so we use the + operator.
    // +Command is syntactic sugar for Command.writeAnsi(out) — see TerminalScope.unaryPlus.
    print("  SetCursorStyle  ")
    for (style in CursorStyle.entries) {
        +SetCursorStyle(style)           // ← + operator: raw Command dispatch
        print(" ${style.name}".darkGrey())
    }
    +SetCursorStyle(CursorStyle.DefaultUserShape)  // restore a sane default
    resetColor()
    println()

    // Terminal commands available as DSL shorthands:
    println("  clear(All|Purge|FromCursorDown|FromCursorUp|CurrentLine|UntilNewLine)")
    println("  scrollUp(n)  scrollDown(n)  setTitle(str)  setSize(cols, rows)")
    println("  enableLineWrap()  disableLineWrap()   enterAlternateScreen() ← you're in it now")
}

private fun TerminalScope.printOscFeatures() {
    // OSC 8 — clickable links.  StartLink and EndLink are Commands;
    // use the + operator (or command()) to dispatch them from inside terminal { }.
    print("  OSC 8  link  ")
    +StartLink("https://github.com/krossterm/krossterm")
    print("krossterm on GitHub".cyan().underlined())
    +EndLink
    println()

    // OSC 52 — clipboard write.  command(c) is identical to +c, just more explicit.
    // Most terminals require the user to opt in (e.g., xterm: allowWindowOps).
    print("  OSC 52 clipboard  ")
    command(CopyToClipboard("krossterm DSL showcase"))  // ← command() form
    println("copied \"krossterm DSL showcase\" → clipboard  (terminal opt-in required)".darkGrey())

    // OSC 9;4 — progress indicator in the taskbar or terminal tab.
    // States: Normal(pct)  Error(pct)  Paused(pct)  Indeterminate  Remove
    // setProgress() is a DSL shorthand for SetProgress(state).writeAnsi(out).
    println("  OSC 9;4 progress  setProgress(Normal|Error|Paused|Indeterminate)  ← animated at startup".darkGrey())

    // sendNotification() is a DSL shorthand for SendNotification(title, body).
    // Targets OSC 9 (ConEmu/Windows Terminal), OSC 99 (Kitty), OSC 777 (urxvt).
    sendNotification("krossterm", "DSL showcase loaded!")
    println("  OSC notification  sendNotification(title, body)  ← check your notification area".darkGrey())
}

private fun TerminalScope.printEscapeHatches() {
    // Three escape hatches for commands not covered by TerminalScope shorthands:

    // 1. + operator — calls Command.writeAnsi(out) via TerminalScope.unaryPlus
    print("  +Command        ")
    print("e.g. +SetCursorStyle(CursorStyle.SteadyBar)".darkGrey())
    println()

    // 2. command(c) — explicit function, equivalent to +c
    print("  command(c)      ")
    print("e.g. command(CopyToClipboard(\"text\"))".darkGrey())
    println()

    // 3. raw(str) — bypass all abstraction and write a literal string to the Writer.
    // Useful for custom or vendor-specific escape sequences.
    print("  raw(str)        ")
    raw("\u001b[2m")          // hand-crafted dim-on escape
    print("e.g. raw(\"\\u001b[2m…\\u001b[0m\") for any literal escape sequence")
    raw("\u001b[0m")          // hand-crafted reset
    println()

    // 4. t.out.execute(...) — imperative API outside a terminal { } block.
    // Queues one or more Commands then immediately flushes the Writer.
    println("  t.out.execute(…)  queue + flush one or more Commands imperatively")
}

# krossterm

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blueviolet.svg)](https://kotlinlang.org/)
[![JVM](https://img.shields.io/badge/jvm-21+-orange.svg)](https://adoptium.net/)

A Kotlin-idiomatic library and DSL for full terminal control on the JVM. Inspired by the Rust [crossterm](https://github.com/crossterm-rs/crossterm) library, krossterm goes beyond ANSI escape codes to provide everything needed for rich TUI applications: raw mode, event streams, mouse capture, synchronized rendering, and a set of shell-integration features that crossterm doesn't offer.

Built on **[jline 3](https://github.com/jline/jline3)** for cross-platform TTY access. Uses **kotlinx.coroutines** `Flow` for events, with a blocking fallback for non-coroutine callers.

---

## Why krossterm?

Most JVM "ANSI" libraries stop at color codes. krossterm is a complete terminal I/O library:

| Capability | krossterm | Typical ANSI lib |
|---|:---:|:---:|
| Raw mode (no line buffering) | Yes | No |
| Keyboard events (key-up, modifiers, media keys) | Yes | No |
| Mouse events (click, drag, scroll) | Yes | No |
| Focus / paste / resize events | Yes | No |
| Alternate screen buffer | Yes | No |
| Synchronized rendering (no flicker) | Yes | No |
| Cursor shape & visibility | Yes | Partial |
| 24-bit RGB + 256 palette + named colors | Yes | Partial |
| OSC 8 links | Yes | No |
| OSC 52 clipboard copy | Yes | No |
| OSC 9;4 progress indicator | Yes | No |
| Desktop notifications | Yes | No |

---

## Requirements

- JDK 21 or newer (built and tested against JDK 25)
- Kotlin 2.3+

---

## Features

### Terminal DSL

A scoped DSL for readable multi-command sequences:

```kotlin
Terminal.system().use { t ->
    t.out.terminal {
        moveTo(0, 0)
        clear()
        setForegroundColor(Color.Cyan)
        println("Hello from krossterm!")
        resetColor()
        moveTo(0, 3)
        print("Press any key...".dim())
    }
    t.readEventBlocking()
}
```

See [`DslShowcase.kt`](examples/src/main/kotlin/io/github/krossterm/examples/DslShowcase.kt) for a single runnable file that exercises every DSL feature: all 16 named colors, RGB and 256-palette swatches, every text attribute, `ContentStyle`, cursor shapes, OSC links, clipboard, progress, notifications, and a live event stream.

---

### Raw mode

Raw mode disables line buffering and echo so your application receives every keystroke immediately, without the user pressing Enter. It is the foundation of any TUI.

```kotlin
Terminal.system().use { t ->
    t.rawMode {
        // Every keypress is delivered immediately; no echo to screen
        val event = t.readEventBlocking()
        println(event)
    }
    // Line-buffered mode is restored automatically on exit
}
```

`rawMode { }` is a scoped function — it restores the terminal on any exit, including exceptions.

---

### Alternate screen

Switch to a clean buffer, draw your UI, and leave no trace in the user's scrollback.

```kotlin
Terminal.system().use { t ->
    t.alternateScreen {
        t.out.execute(Clear(ClearType.All), MoveTo(0, 0))
        t.out.execute(Print("Press any key to exit..."))
        t.readEventBlocking()
    }
    // Original screen and scrollback are fully restored
}
```

---

### Style and color

Three color spaces, a full set of SGR attributes, and composable extension functions.

```kotlin
// Named colors
t.out.execute(Print("error".red().bold()))

// 24-bit RGB
t.out.execute(Print("custom".with(Color.Rgb(255, 165, 0))))

// 256-color palette
t.out.execute(Print("palette".with(Color.AnsiValue(208))))

// Hex strings — Color.parse() accepts #rrggbb, #rgb, named, rgb(r,g,b), ansi(n)
t.out.execute(Print("hex".with(Color.parse("#e63946")!!)))

// Layered: foreground + background + underline + attributes
t.out.execute(Print("fancy".white().on(Color.DarkBlue).bold().italic()))

// Or use Commands directly
t.out.execute(
    SetForegroundColor(Color.Green),
    SetAttribute(Attribute.Bold),
    Print("styled"),
    ResetColor,
)
```

---

### Cursor control

```kotlin
t.out.execute(
    MoveTo(10, 5),          // absolute (column, row)
    MoveRight(3),           // relative
    SavePosition,
    Hide,
    SetCursorStyle.SteadyBar,
)

// Query actual cursor position
val pos: Position? = t.cursorPosition()
```

---

### Keyboard events

krossterm delivers rich keyboard events including key-down, key-up, key-repeat, all modifiers, function keys, media keys, and the full [Kitty keyboard protocol](https://sw.kovidgoyal.net/kitty/keyboard-protocol/).

```kotlin
Terminal.system().use { t ->
    t.rawMode {
        runBlocking {
            t.events()
                .takeWhile { event ->
                    // Stop on Escape
                    event !is Event.Key || event.event.code != KeyCode.Esc
                }
                .collect { event ->
                    when (event) {
                        is Event.Key -> {
                            val key = event.event
                            println("${key.kind} ${key.code} mod=${key.modifiers}")
                        }
                        else -> println(event)
                    }
                }
        }
    }
}
```

Enable Kitty progressive enhancement for key-up events and unambiguous modifier keys:

```kotlin
t.out.execute(PushKeyboardEnhancementFlags(
    KeyboardEnhancementFlags.DISAMBIGUATE_ESCAPE_CODES +
    KeyboardEnhancementFlags.REPORT_EVENT_TYPES
))
```

---

### Mouse events

```kotlin
t.out.execute(EnableMouseCapture)

t.rawMode {
    runBlocking {
        t.events().collect { event ->
            if (event is Event.Mouse) {
                val m = event.event
                println("${m.kind} at col=${m.column} row=${m.row}")
            }
        }
    }
}

t.out.execute(DisableMouseCapture)
```

Supported event kinds: `Down`, `Up`, `Drag` (per button), `Moved`, `ScrollUp`, `ScrollDown`, `ScrollLeft`, `ScrollRight`.

---

### Synchronized rendering

Batch output changes into a single atomic update — eliminates tearing and flicker in complex UIs.

```kotlin
t.synchronizedUpdate {
    t.out.execute(
        MoveTo(0, 0), Clear(ClearType.All),
        Print("frame $n rendered atomically"),
    )
}
```

---

### Links (OSC 8)

Emit clickable links in terminals that support [OSC 8](https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda).

```kotlin
// .link() extension — composes with the Stylize chain
println("krossterm on GitHub".bold().cyan().link("https://github.com/krossterm/krossterm"))

// Imperative style
t.out.execute(
    StartLink("https://github.com/krossterm/krossterm"),
    Print("krossterm on GitHub"),
    EndLink,
)

// DSL style — use the + operator (StartLink has no TerminalScope shorthand)
t.out.terminal {
    +StartLink("https://github.com/krossterm/krossterm")
    print("krossterm on GitHub")
    +EndLink
}
```

The optional `params` map (most commonly `id=`) coalesces link segments that span a line break into one clickable target.

---

### Clipboard (OSC 52)

Copy text to the system clipboard without spawning a subprocess.

```kotlin
t.out.execute(CopyToClipboard("copied text"))

// Also write to the X11 primary selection
t.out.execute(CopyToClipboard(
    content = "selected text",
    destinations = setOf(ClipboardDestination.Clipboard, ClipboardDestination.Primary),
))
```

---

### Progress indicator (OSC 9;4)

Show progress in the taskbar or terminal tab using the [OSC 9;4](https://conemu.github.io/en/AnsiEscapeCodes.html#ConEmu_specific_OSC) protocol, supported by Windows Terminal, ConEmu, and others.

```kotlin
// Show a determinate progress bar at 42%
t.out.execute(SetProgress(ProgressState.Normal(42)))

// Signal an error state
t.out.execute(SetProgress(ProgressState.Error(42)))

// Indeterminate (spinning)
t.out.execute(SetProgress(ProgressState.Indeterminate))

// Paused
t.out.execute(SetProgress(ProgressState.Paused(75)))

// Remove the indicator
t.out.execute(ClearProgress)
```

---

### Desktop notifications

Trigger a desktop notification from within the terminal, without any OS-specific API or subprocess.

```kotlin
t.out.execute(SendNotification(
    title = "Build complete",
    body = "All tests passed in 4.2 s",
))
```

Emits OSC 9 (ConEmu / Windows Terminal), OSC 99 (Kitty), and OSC 777 (urxvt + libnotify) for broad terminal coverage.

---

## Running the examples

Nine runnable examples are included under `examples/src/main/kotlin/io/github/krossterm/examples/`:

```bash
./gradlew runExample -Pexample=KeyDisplay
```

| Example | Demonstrates |
|---|---|
| `DslShowcase` | **Every DSL feature in one file** — colors, attributes, cursor, OSC, events |
| `IsTty` | TTY detection, terminal type, and size |
| `Stylize` | Named / palette / RGB colors and attribute combos |
| `Link` | OSC 8 links — imperative, DSL, and `.link()` extension |
| `KeyDisplay` | Raw mode + `Flow<Event>` + all key event fields |
| `EventStreamCoroutines` | `pollEvent(timeout)` heartbeat pattern |
| `InteractiveDemo` | Alternate screen, synchronized updates, mouse capture |
| `Progress` | All four OSC 9;4 progress states |
| `Notify` | Desktop notifications via OSC 9 / 99 / 777 |

---

## Design notes

- **`Command` is a `fun interface`** — every action implements `writeAnsi(out: Appendable)`. Custom commands are a single SAM lambda.
- **`Attributes`, `KeyModifiers`, `KeyEventState`** are `@JvmInline value class` bitsets — zero allocation, `+` / `-` / `in` operators.
- **`Stylize` extensions** on `String` and `StyledContent<T>` compose via chaining: `"warn".yellow().bold().on(Color.Black)`.
- **Event delivery** uses `MutableSharedFlow` with a 256-element buffer — keystrokes are never dropped under backpressure.
- **ANSI input parser** is hand-rolled and byte-by-byte, ported from crossterm's `src/event/sys/unix/parse.rs`. Supports Kitty CSI-u, SGR mouse, bracketed paste, and cursor-position responses.
- **Scoped resource management**: `rawMode { }`, `alternateScreen { }`, and `synchronizedUpdate { }` are inline functions that guarantee cleanup on any exit path, including exceptions.
- **Single module, single jar** — `io.github.krossterm:krossterm`.

---

## Testing

```bash
./gradlew test
```

123 tests cover the ANSI parser, every command's escape sequence, `Stylize` / `Attributes`, and integration scopes.

TTY-required tests are tagged `Tty` and excluded by default. Run them locally with a real terminal attached:

```bash
./gradlew ttyTest
```
---
## License

MIT — see [LICENSE](LICENSE).

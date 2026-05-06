# krossterm

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blueviolet.svg)](https://kotlinlang.org/)
[![JVM](https://img.shields.io/badge/jvm-21+-orange.svg)](https://adoptium.net/)

Pure-Kotlin reimplementation of the Rust [crossterm](https://github.com/crossterm-rs/crossterm) library — cross-platform terminal manipulation for the JVM.

## What's in the box

- **Cursor** — move, save / restore, hide / show, blink, shape
- **Terminal** — clear, scroll, resize, alternate screen, synchronized output, line wrap
- **Style** — 16 named colors, 256-color palette, 24-bit RGB, foreground / background / underline, attributes (bold, italic, underline, etc.)
- **Stylize** — extension functions: `"warn".yellow().bold().on(Color.Black)`
- **Events** — keyboard / mouse / focus / paste / resize as `Flow<Event>` or blocking `readEvent()`
- **Kitty keyboard protocol** — push / pop progressive enhancement flags; CSI-u parsing
- **Bracketed paste** — clipboard content arrives as `Event.Paste`
- **Hyperlinks** — OSC 8 clickable URIs with key=value params
- **Clipboard** — OSC 52 copy with multi-destination (clipboard + primary)

Built on **jline 3** for cross-platform raw mode and TTY access. Uses **kotlinx.coroutines** `Flow` for events, with a blocking escape hatch for non-coroutine callers.

## Requirements

- JDK 21 or newer (JDK 25 recommended; krossterm is built and tested against 25)
- Kotlin 2.3+ for consumers

## Quick start

```kotlin
import io.github.krossterm.*
import io.github.krossterm.cursor.*
import io.github.krossterm.style.*
import io.github.krossterm.terminal.*

fun main() = Terminal.system().use { t ->
    t.alternateScreen {
        t.out.execute(MoveTo(0, 0), Print("hello, ".bold()), Print("world".red().underlined()))
        Thread.sleep(1500)
    }
}
```

DSL form:

```kotlin
Terminal.system().use { t ->
    t.out.terminal {
        moveTo(0, 0)
        print("hello, ".bold())
        print("world".red().underlined())
    }
}
```

Events with coroutines:

```kotlin
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking

Terminal.system().use { t ->
    t.rawMode {
        runBlocking {
            t.events()
                .takeWhile { it !is Event.Key || it.event.code != KeyCode.Esc }
                .collect { println(it) }
        }
    }
}
```

## Examples

Six runnable examples live under `examples/src/main/kotlin/io/github/krossterm/examples/`. Run any one with:

```bash
./gradlew runExample -Pexample=KeyDisplay
```

| Example | Demonstrates |
|---|---|
| `IsTty` | TTY detection and terminal size |
| `Stylize` | Named / palette / RGB colors, attribute combos |
| `Link` | OSC 8 hyperlinks |
| `KeyDisplay` | Raw mode + `Flow<Event>` |
| `EventStreamCoroutines` | `pollEvent(timeout)` heartbeat pattern |
| `InteractiveDemo` | Alternate screen + sync update + mouse capture + style |

## Design notes

- **Single module**, single jar (`io.github.krossterm:krossterm`).
- **`Command` is a `fun interface`** — every action implements one method, `writeAnsi(out: Appendable)`. Custom commands are one SAM lambda away.
- **DSL marker** prevents accidental nesting of `terminal { terminal { … } }`.
- **`Stylize` extensions** on `String` and `StyledContent<T>` so `.red().bold()` chains compose cleanly.
- **`Attributes` is a value class** wrapping `Int` — zero-allocation bitset with `+` / `-` / `in` operators.
- **Events**: `MutableSharedFlow<Event>` with backpressure-suspend (256-element buffer); never drops keystrokes.
- **Parser** is hand-rolled, byte-by-byte, ported from crossterm's `src/event/sys/unix/parse.rs` — supports kitty CSI-u, SGR mouse, bracketed paste.
- **Resource lifetime** is `AutoCloseable` + `use { }`. `rawMode { }`, `alternateScreen { }`, `synchronizedUpdate { }` are inline scope functions that restore on any exit.

## Status

Early-development port. The public surface is stable enough to use; expect the occasional new attribute or terminal feature to land. Tracked milestones:

- [x] M1 — skeleton + Command + DSL + cursor / terminal commands
- [x] M2 — Style + Stylize
- [x] M3 — Event types + ANSI input parser (84 corpus tests)
- [x] M4 — EventReader + Flow + raw mode + SIGWINCH + cursor query
- [x] M5 — Clipboard (OSC 52) + hyperlinks (OSC 8)
- [x] M6 — Examples + KDoc + README
- [ ] M7 — CI matrix + Maven Central publishing

## Testing

```bash
./gradlew test
```

Currently 123 tests across parser, command serialization, Stylize, Attributes, and integration scopes.

TTY-required integration tests are tagged `Tty` and excluded by default. To run them locally:

```bash
./gradlew ttyTest
```

## License

MIT — see [LICENSE](LICENSE).

package io.github.krossterm.examples;

import io.github.krossterm.Command;
import io.github.krossterm.CommandKt;
import io.github.krossterm.Terminal;
import io.github.krossterm.clipboard.ClipboardDestination;
import io.github.krossterm.clipboard.CopyToClipboard;
import io.github.krossterm.cursor.CursorStyle;
import io.github.krossterm.cursor.Hide;
import io.github.krossterm.cursor.SetCursorStyle;
import io.github.krossterm.cursor.Show;
import io.github.krossterm.event.DisableFocusChange;
import io.github.krossterm.event.DisableMouseCapture;
import io.github.krossterm.event.EnableFocusChange;
import io.github.krossterm.event.EnableMouseCapture;
import io.github.krossterm.event.Event;
import io.github.krossterm.event.KeyCode;
import io.github.krossterm.event.MouseEvent;
import io.github.krossterm.link.EndLink;
import io.github.krossterm.link.StartLink;
import io.github.krossterm.notification.SendNotification;
import io.github.krossterm.progress.ProgressState;
import io.github.krossterm.progress.SetProgress;
import io.github.krossterm.style.Attribute;
import io.github.krossterm.style.Color;
import io.github.krossterm.style.ResetColor;
import io.github.krossterm.style.SetAttribute;
import io.github.krossterm.style.SetBackgroundColor;
import io.github.krossterm.style.SetColors;
import io.github.krossterm.style.SetForegroundColor;
import io.github.krossterm.style.SetUnderlineColor;
import io.github.krossterm.style.StylizeKt;
import io.github.krossterm.terminal.Clear;
import io.github.krossterm.terminal.ClearType;
import io.github.krossterm.terminal.EnableLineWrap;
import io.github.krossterm.cursor.MoveTo;
import io.github.krossterm.terminal.SetTitle;
import kotlin.Unit;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * Java equivalent of DslShowcase — the same sections using krossterm's imperative API.
 *
 * Key differences from Kotlin:
 *  - try-with-resources instead of .use { }
 *  - CommandKt.execute(t.getOut(), ...) instead of t.out.execute(...)
 *  - StylizeKt.bold("text") instead of "text".bold()   (extension fns → static methods)
 *  - Color.Red.INSTANCE for named color singletons      (data objects → INSTANCE fields)
 *  - Color.Companion.parse("#rrggbb") for hex parsing   (companion fns → Companion.method)
 *  - t.readEventBlocking() instead of suspend readEvent() (no coroutines needed)
 *  - () -> { ...; return Unit.INSTANCE; } for Kotlin lambda parameters
 *
 * Run with: ./gradlew runExample -Pexample=JavaShowcase
 */
public class JavaShowcase {

    public static void main(String[] args) throws Exception {
        try (Terminal t = Terminal.system()) {
            exec(t, new SetTitle("krossterm Java showcase"));

            // alternateScreen / rawMode take kotlin.jvm.functions.Function0 — Java lambdas work.
            t.alternateScreen(() -> {
                t.rawMode(() -> {
                    Thread hook = new Thread(() -> {
                        try { exec(t, DisableMouseCapture.INSTANCE, Show.INSTANCE); }
                        catch (Exception ignored) {}
                    });
                    Runtime.getRuntime().addShutdownHook(hook);

                    try {
                        exec(t, EnableMouseCapture.INSTANCE, EnableFocusChange.INSTANCE, Hide.INSTANCE);

                        // OSC 9;4 — progress animation while the showcase "loads"
                        for (int pct = 0; pct <= 100; pct += 25) {
                            exec(t, new SetProgress(new ProgressState.Normal(pct)));
                            Thread.sleep(120);
                        }
                        exec(t, new SetProgress(ProgressState.Remove.INSTANCE));

                        // synchronizedUpdate — render the whole frame atomically (no flicker)
                        t.synchronizedUpdate(() -> {
                            exec(t, new Clear(ClearType.All), new MoveTo(0, 0));
                            printHeader(t);
                            section(t, "COLORS — 16 named · RGB truecolor · 256-color palette · hex strings");
                            printColors(t);
                            section(t, "ATTRIBUTES");
                            printAttributes(t);
                            section(t, "STYLE COMMANDS — SetForegroundColor / SetBackgroundColor / SetAttribute");
                            printStyleCommands(t);
                            section(t, "CURSOR & TERMINAL COMMANDS");
                            printCursorAndTerminal(t);
                            section(t, "OSC FEATURES — links · clipboard · progress · notifications");
                            printOscFeatures(t);
                            section(t, "EVENT STREAM — press keys · click · scroll   (Esc to quit)");
                            return Unit.INSTANCE;
                        });

                        // Event loop — readEventBlocking() is the Java-friendly variant
                        // (readEvent() is a suspend fun; use it from Kotlin with coroutines)
                        while (true) {
                            Event event = t.readEventBlocking();
                            if (event instanceof Event.Key keyEv
                                    && keyEv.getEvent().getCode() instanceof KeyCode.Esc) break;
                            printEvent(t, event);
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        exec(t, DisableMouseCapture.INSTANCE, DisableFocusChange.INSTANCE,
                             Show.INSTANCE, new SetProgress(ProgressState.Remove.INSTANCE));
                        try { Runtime.getRuntime().removeShutdownHook(hook); }
                        catch (Exception ignored) {}
                    }
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
        }
    }

    // ─── Low-level helpers ────────────────────────────────────────────────────

    /** Execute one or more Commands and flush. */
    private static void exec(Terminal t, Command... cmds) {
        CommandKt.execute(t.getOut(), cmds);
    }

    /** Write any value (calls toString()) and flush — no newline. */
    private static void write(Terminal t, Object value) {
        try {
            Writer out = t.getOut();
            out.write(value.toString());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Write a value followed by a newline and flush. */
    private static void writeln(Terminal t, Object value) {
        write(t, value.toString() + "\n");
    }

    /** Print a dark-grey section separator that fills 78 columns. */
    private static void section(Terminal t, String title) {
        exec(t, ResetColor.INSTANCE);
        StringBuilder sb = new StringBuilder("── ").append(title).append(" ");
        while (sb.length() < 78) sb.append('─');
        writeln(t, StylizeKt.darkGrey(sb.toString()));
    }

    // ─── Content sections ─────────────────────────────────────────────────────

    private static void printHeader(Terminal t) {
        // StylizeKt.bold() is the Java name for "text".bold() in Kotlin.
        // Chaining StylizeKt.cyan(StylizeKt.bold(...)) mirrors "text".bold().cyan().
        write(t, "  ");
        write(t, StylizeKt.cyan(StylizeKt.bold("krossterm Java showcase")));
        write(t, StylizeKt.darkGrey("   CommandKt.execute(t.getOut(), ...)"));
        writeln(t, StylizeKt.darkGrey(
            "   [" + t.getSize().getColumns() + "×" + t.getSize().getRows() + "]"));
        writeln(t, "");
    }

    private static void printColors(Terminal t) {
        String blk = "█";

        // Named foreground colors — data objects expose a public INSTANCE field in Java
        write(t, "  fg  ");
        for (Color c : new Color[]{
                Color.Black.INSTANCE, Color.DarkRed.INSTANCE, Color.DarkGreen.INSTANCE,
                Color.DarkYellow.INSTANCE, Color.DarkBlue.INSTANCE, Color.DarkMagenta.INSTANCE,
                Color.DarkCyan.INSTANCE, Color.Grey.INSTANCE})
            write(t, StylizeKt.with(blk + " ", c));
        writeln(t, "");

        write(t, "       ");
        for (Color c : new Color[]{
                Color.DarkGrey.INSTANCE, Color.Red.INSTANCE, Color.Green.INSTANCE,
                Color.Yellow.INSTANCE, Color.Blue.INSTANCE, Color.Magenta.INSTANCE,
                Color.Cyan.INSTANCE, Color.White.INSTANCE})
            write(t, StylizeKt.with(blk + " ", c));
        writeln(t, "");

        // Background — StylizeKt.on(str, Color) is "str".on(Color) in Kotlin
        write(t, "  bg  ");
        for (Color c : new Color[]{
                Color.Black.INSTANCE, Color.DarkRed.INSTANCE, Color.DarkGreen.INSTANCE, Color.DarkYellow.INSTANCE,
                Color.DarkBlue.INSTANCE, Color.DarkMagenta.INSTANCE, Color.DarkCyan.INSTANCE, Color.Grey.INSTANCE,
                Color.DarkGrey.INSTANCE, Color.Red.INSTANCE, Color.Green.INSTANCE, Color.Yellow.INSTANCE,
                Color.Blue.INSTANCE, Color.Magenta.INSTANCE, Color.Cyan.INSTANCE, Color.White.INSTANCE})
            write(t, StylizeKt.on("  ", c));
        writeln(t, "");

        // 24-bit truecolor — new Color.Rgb(r, g, b)
        write(t, "  RGB ");
        int w = 72;
        for (int i = 0; i < w; i++) {
            int r = i * 255 / w, g = (w - i) * 255 / w, b = 200 - i * 150 / w;
            write(t, StylizeKt.with(blk, new Color.Rgb(r, g, b)));
        }
        writeln(t, "");

        // 256-color palette — new Color.AnsiValue(n)
        write(t, "  256 ");
        for (int n = 0; n < 16; n++) write(t, StylizeKt.with(blk, new Color.AnsiValue(n)));
        write(t, "  ");
        for (int n = 196; n < 232; n += 2) write(t, StylizeKt.with(blk, new Color.AnsiValue(n)));
        write(t, "  ");
        for (int n = 232; n <= 255; n++) write(t, StylizeKt.with(blk, new Color.AnsiValue(n)));
        writeln(t, "");

        // Hex strings — Color.parse() lives on the companion object; call via Color.Companion
        write(t, "  hex ");
        for (String hex : new String[]{"#f00","#f80","#ff0","#0f0","#0ff","#00f","#80f","#f0f",
                                        "#c00","#c60","#cc0","#0c0","#0cc","#00c","#60c","#c0c"})
            write(t, StylizeKt.with(blk, Color.Companion.parse(hex)));
        write(t, "  ");
        for (String hex : new String[]{"#e63946","#457b9d","#2a9d8f","#e9c46a","#f4a261","#264653"})
            write(t, StylizeKt.with("  " + hex + "  ", Color.Companion.parse(hex)));
        writeln(t, "");
    }

    private static void printAttributes(Terminal t) {
        write(t, "  ");
        // StylizeKt extension functions work on both String and StyledContent<D>
        write(t, StylizeKt.bold("bold "));
        write(t, StylizeKt.dim("dim "));
        write(t, StylizeKt.italic("italic "));
        write(t, StylizeKt.underlined("underlined "));
        write(t, StylizeKt.doubleUnderlined("double-underlined "));
        write(t, StylizeKt.slowBlink("slowBlink "));
        write(t, StylizeKt.crossedOut("crossedOut "));
        writeln(t, StylizeKt.overLined("overLined"));

        write(t, "  ");
        write(t, StylizeKt.framed("framed "));
        write(t, StylizeKt.encircled("encircled "));
        write(t, StylizeKt.reverse("reverse "));
        // Chaining on StyledContent: StylizeKt.italic(StylizeKt.bold(...))
        write(t, StylizeKt.italic(StylizeKt.bold("bold+italic ")));
        writeln(t, StylizeKt.underlined(StylizeKt.italic(StylizeKt.bold("bold+italic+underlined"))));

        // Underline color — stateful commands; pair with ResetColor when done
        write(t, "  underline color  ");
        exec(t, new SetUnderlineColor(Color.Red.INSTANCE),    new SetAttribute(Attribute.Underlined));
        write(t, "red  ");
        exec(t, new SetUnderlineColor(Color.Green.INSTANCE)); write(t, "green  ");
        exec(t, new SetUnderlineColor(Color.Blue.INSTANCE));  write(t, "blue");
        exec(t, ResetColor.INSTANCE);
        writeln(t, "");
    }

    private static void printStyleCommands(Terminal t) {
        // Individual set* commands — the Java-natural approach without ContentStyle/Attributes bitsets
        exec(t, new SetForegroundColor(Color.White.INSTANCE),
             new SetBackgroundColor(Color.DarkBlue.INSTANCE),
             new SetAttribute(Attribute.Bold),
             new SetAttribute(Attribute.Italic));
        write(t, "  SetForeground(White) SetBackground(DarkBlue) SetAttribute(Bold) SetAttribute(Italic)  ");
        exec(t, ResetColor.INSTANCE);
        writeln(t, "");

        // SetColors — fg + bg atomically in a single SGR
        exec(t, new SetColors(Color.Black.INSTANCE, Color.Cyan.INSTANCE));
        write(t, "  SetColors(fg=Black, bg=Cyan)  ");
        exec(t, ResetColor.INSTANCE);
        writeln(t, "");
    }

    private static void printCursorAndTerminal(Terminal t) {
        writeln(t, "  new MoveTo(col, row)   new MoveUp(n) / MoveDown / MoveLeft / MoveRight");
        writeln(t, "  new MoveToColumn(n)    SavePosition.INSTANCE   RestorePosition.INSTANCE");
        writeln(t, "  Hide.INSTANCE / Show.INSTANCE   cursor currently hidden; Show called in finally");

        // SetCursorStyle — cycle through all shapes using CursorStyle.values() (Java enum API)
        write(t, "  SetCursorStyle  ");
        for (CursorStyle style : CursorStyle.values()) {
            exec(t, new SetCursorStyle(style));
            write(t, StylizeKt.darkGrey(" " + style.name()));
        }
        exec(t, new SetCursorStyle(CursorStyle.DefaultUserShape));
        exec(t, ResetColor.INSTANCE);
        writeln(t, "");

        writeln(t, "  new Clear(ClearType.All|Purge|FromCursorDown|FromCursorUp|CurrentLine|UntilNewLine)");
        writeln(t, "  new ScrollUp(n)  new SetTitle(str)  new SetSize(cols, rows)  " +
                   EnableLineWrap.INSTANCE.getClass().getSimpleName() + ".INSTANCE");
    }

    private static void printOscFeatures(Terminal t) {
        // OSC 8 — new StartLink(uri, params) / EndLink.INSTANCE
        // params requires all arguments in Java (no default params from Kotlin data classes)
        write(t, "  OSC 8  link  ");
        exec(t, new StartLink("https://github.com/krossterm/krossterm", Map.of()));
        write(t, StylizeKt.underlined(StylizeKt.cyan("krossterm on GitHub")));
        exec(t, EndLink.INSTANCE);
        writeln(t, "");

        // OSC 52 — all CopyToClipboard constructor arguments required from Java
        write(t, "  OSC 52 clipboard  ");
        exec(t, new CopyToClipboard<>(
            "krossterm Java showcase",
            Set.of(ClipboardDestination.Clipboard.INSTANCE),
            100_000
        ));
        writeln(t, StylizeKt.darkGrey("copied to clipboard (terminal opt-in required)"));

        // OSC 9;4 — progress states
        writeln(t, StylizeKt.darkGrey(
            "  OSC 9;4 progress  new SetProgress(new ProgressState.Normal(pct))  ← animated at startup"));

        // OSC 9/99/777 — desktop notification
        exec(t, new SendNotification("krossterm", "Java showcase loaded!"));
        writeln(t, StylizeKt.darkGrey(
            "  OSC notification  new SendNotification(title, body)  ← check your notification area"));
    }

    private static void printEvent(Terminal t, Event event) {
        // Java 16+ pattern matching instanceof — requires JDK 21+ (this project's minimum)
        String line;
        if (event instanceof Event.Key keyEv) {
            var key = keyEv.getEvent();
            // KeyModifiers is a Kotlin value class — its getter is mangled in JVM bytecode.
            // Access code and kind directly; omit modifiers in this Java example.
            line = StylizeKt.yellow(
                "  key    " + key.getCode() + "  [" + key.getKind() + "]").toString();
        } else if (event instanceof Event.Mouse mouseEv) {
            MouseEvent m = mouseEv.getEvent();
            line = StylizeKt.cyan(
                "  mouse  " + m.getKind() + "  col=" + m.getColumn() + " row=" + m.getRow()).toString();
        } else if (event instanceof Event.Resize resizeEv) {
            line = StylizeKt.magenta(
                "  resize " + resizeEv.getColumns() + "×" + resizeEv.getRows()).toString();
        } else if (event instanceof Event.Paste pasteEv) {
            String text = pasteEv.getText();
            line = StylizeKt.green(
                "  paste  \"" + text.substring(0, Math.min(50, text.length())) + "\"").toString();
        } else if (event instanceof Event.FocusGained) {
            line = StylizeKt.white("  focus gained").toString();
        } else {
            line = StylizeKt.darkGrey("  focus lost").toString();
        }
        writeln(t, line);
    }
}

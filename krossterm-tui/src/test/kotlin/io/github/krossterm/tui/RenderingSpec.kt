package io.github.krossterm.tui

import io.github.krossterm.style.bold
import io.github.krossterm.style.green
import io.github.krossterm.style.red
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class RenderingSpec : StringSpec({

    "visibleWidth strips ANSI CSI sequences" {
        visibleWidth("hello") shouldBe 5
        visibleWidth("hi".red().toString()) shouldBe 2
        visibleWidth("warn".bold().red().toString()) shouldBe 4
    }

    "padOrTruncate pads short lines" {
        padOrTruncate("hi", 5) shouldBe "hi   "
    }

    "padOrTruncate preserves ANSI when padding" {
        val styled = "hi".red().toString()
        val padded = padOrTruncate(styled, 5)
        visibleWidth(padded) shouldBe 5
        padded shouldContain styled
    }

    "padOrTruncate truncates and resets styling" {
        val styled = "hello".red().toString()
        val truncated = padOrTruncate(styled, 3)
        visibleWidth(truncated) shouldBe 3
        // Truncation must close styling so it doesn't leak.
        truncated shouldContain RESET
    }

    "Text wraps long lines without leaking ANSI styles into padding" {
        val component = text("worker ${"down".red()}")
        val lines = component.render(8)
        lines shouldHaveSize 2
        // Each line is exactly 8 visible columns (right-padded).
        lines.forEach { visibleWidth(it) shouldBe 8 }
        // The first line is just "worker  " (no styling at all).
        lines[0] shouldNotContain ESC.toString()
    }

    "Frame renders with title, padding, and border" {
        val f = frame {
            title = "Hi"
            padding = 1
            borderStyle = BorderStyle.Rounded
            +text("body")
        }
        val lines = f.render(10)
        lines shouldHaveSize 5  // top + pad + body + pad + bottom
        lines[0] shouldContain "Hi"
        lines[0].startsWith(BorderStyle.Rounded.topLeft) shouldBe true
        lines.last().startsWith(BorderStyle.Rounded.bottomLeft) shouldBe true
        // every line is exactly 10 columns
        lines.forEach { visibleWidth(it) shouldBe 10 }
    }

    "Table auto-sizes columns to widest cell" {
        val t = table {
            headers("X", "Long Header")
            row("a", "b")
            row("aaaaa", "c")
        }
        val lines = t.render(t.preferredWidth())
        // Header row has both "X" and "Long Header" visible, padded by columns.
        lines.any { it.contains("Long Header") } shouldBe true
        // Body row has the wider cell expanded.
        lines.any { it.contains("aaaaa") } shouldBe true
    }

    "Row layout splits width by weight and aligns rows by zipping" {
        val r = row {
            cell(weight = 1) { text("AAAA") }
            cell(weight = 1) { text("BBBB") }
        }
        val lines = r.render(10)
        lines shouldHaveSize 1
        visibleWidth(lines[0]) shouldBe 10
        lines[0] shouldContain "AAAA"
        lines[0] shouldContain "BBBB"
    }

    "Row layout pads shorter children with blank lines" {
        val tall = column {
            +text("1")
            +text("2")
            +text("3")
        }
        val short = text("X")
        val r = row {
            cell(weight = 1) { tall }
            cell(weight = 1) { short }
        }
        val lines = r.render(4)
        lines shouldHaveSize 3
        // Right side of the second and third row should be all spaces (width 2).
        lines[1].endsWith("  ") shouldBe true
        lines[2].endsWith("  ") shouldBe true
    }

    "ProgressBar shows label, bar, and percent within target width" {
        val p = progressBar(value = 50, total = 100, label = "ETA")
        val lines = p.render(30)
        lines shouldHaveSize 1
        visibleWidth(lines[0]) shouldBe 30
        lines[0] shouldContain "ETA"
        lines[0] shouldContain "50%"
    }

    "ProgressBar Smooth uses partial blocks for fractional progress" {
        // value/total = 1/64 -> 1 sub-cell out of (8 cells * 8 buckets/cell = 64).
        // -> 0 full cells, partial index 1 -> first glyph is the 1/8 block.
        val p = progressBar(value = 1, total = 64, style = ProgressStyle.Smooth, showPercent = false, label = null, brackets = null, width = 8)
        val rendered = p.render(8)[0]
        rendered.first() shouldBe '▏'
    }

    "Divider with title fills width with horizontal glyphs" {
        val d = divider(title = "Section", preferredWidth = 20)
        val rendered = d.render(20)[0]
        visibleWidth(rendered) shouldBe 20
        rendered shouldContain "Section"
    }

    "terminalWidth returns a positive value (live or fallback)" {
        val w = terminalWidth(fallback = 123)
        (w > 0) shouldBe true
    }

    "isInteractive is false when running under the test runner (no console)" {
        isInteractive() shouldBe false
    }

    "Frame body styling does not leak across rows" {
        val f = frame {
            +text("hi ${"there".green()}")
            +text("plain")
        }
        val lines = f.render(f.preferredWidth())
        // Body line containing "plain" should have no green styling at its end.
        val plainLine = lines.first { it.contains("plain") }
        // The styling within the prior row must not leak; the plain row should
        // contain a closing reset or no ANSI escapes after its content.
        plainLine shouldNotContain "[32m"
    }
})

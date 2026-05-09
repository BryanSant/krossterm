package io.github.krossterm.tui

import io.github.krossterm.style.green
import io.github.krossterm.style.red
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.StringWriter

class NoColorSpec : StringSpec({

    "stripSgr removes SGR sequences" {
        val styled = "hi ${"there".red()}"
        stripSgr(styled) shouldBe "hi there"
    }

    "stripSgr preserves cursor-movement CSI escapes" {
        // CSI sequences ending in non-'m' bytes (cursor motion, clear-line, etc.)
        // must pass through. Use ESC literal for clarity.
        val esc = ESC.toString()
        val withCursor = "${esc}[1Ghello${esc}[K"
        stripSgr(withCursor) shouldBe withCursor
    }

    "stripSgr preserves OSC hyperlinks" {
        val esc = ESC.toString()
        val link = "${esc}]8;;https://example.com${esc}\\click${esc}]8;;${esc}\\"
        stripSgr(link) shouldBe link
    }

    "stripSgr removes SGR but keeps surrounding cursor escapes" {
        val esc = ESC.toString()
        val mixed = "${esc}[1G${esc}[31mhello${esc}[0m${esc}[K"
        stripSgr(mixed) shouldBe "${esc}[1Ghello${esc}[K"
    }

    "Component.toRenderedString strips colors when stripColor = true" {
        val component = text("hi ${"there".red()}")
        val raw = component.toRenderedString(stripColor = false)
        val stripped = component.toRenderedString(stripColor = true)
        raw shouldContain ESC.toString()
        stripped shouldNotContain ESC.toString()
        stripped shouldContain "hi there"
    }

    "Component.writeTo strips colors when stripColor = true" {
        val out = StringWriter()
        text("hi ${"there".green()}").writeTo(out, stripColor = true)
        out.toString() shouldNotContain ESC.toString()
        out.toString() shouldContain "hi there"
    }

    "Component.writeTo keeps colors when stripColor = false" {
        val out = StringWriter()
        text("hi ${"there".green()}").writeTo(out, stripColor = false)
        out.toString() shouldContain ESC.toString()
    }

    "withSpinner non-interactive strips SGR from labels when stripColor = true" {
        val out = StringWriter()
        runBlocking {
            withSpinner(
                "loading ${"now".red()}",
                out = out,
                interactive = false,
                stripColor = true,
            ) { spinner ->
                delay(120)
                spinner.label = "halfway ${"on".green()}"
                delay(120)
            }
        }
        val output = out.toString()
        output shouldNotContain ESC.toString()
        output shouldContain "loading now"
        output shouldContain "halfway on"
    }

    "noColor reflects the NO_COLOR env var (or its absence)" {
        // We can't reliably set environment variables from JVM tests, so we just
        // exercise the function returns a Boolean that matches what System.getenv
        // says right now. This catches accidental hard-coded returns.
        val expected = System.getenv("NO_COLOR")?.isNotEmpty() == true
        noColor() shouldBe expected
    }
})

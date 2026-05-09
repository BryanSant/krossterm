package io.github.krossterm.tui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.StringWriter

class SpinnerSpec : StringSpec({

    "withSpinner non-interactive prints initial label and each label change as a separate line" {
        val out = StringWriter()
        runBlocking {
            withSpinner("Loading", out = out, interactive = false) { spinner ->
                delay(120)
                spinner.label = "Halfway"
                delay(120)
                spinner.label = "Almost"
                delay(120)
            }
        }
        val output = out.toString()
        output shouldContain "Loading\n"
        output shouldContain "Halfway\n"
        output shouldContain "Almost\n"
        // No ANSI cursor-hide / clear-line escape codes leaked.
        output shouldNotContain "[?25l"
        output shouldNotContain "[K"
    }

    "withSpinner non-interactive does not duplicate unchanged labels" {
        val out = StringWriter()
        runBlocking {
            withSpinner("Same", out = out, interactive = false) {
                delay(200)
            }
        }
        // "Same" should appear exactly once (initial print, no animation).
        val occurrences = out.toString().split("Same").size - 1
        occurrences shouldBe 1
    }

    "withSpinner returns the block's value" {
        val out = StringWriter()
        val result = runBlocking {
            withSpinner("Working", out = out, interactive = false) { 42 }
        }
        result shouldBe 42
    }
})

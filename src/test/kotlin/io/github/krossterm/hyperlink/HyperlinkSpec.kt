package io.github.krossterm.hyperlink

import io.github.krossterm.Command
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun ansi(c: Command): String = StringBuilder().also(c::writeAnsi).toString()

class HyperlinkSpec : StringSpec({

    val esc = "\u001B"

    "StartHyperlink with no params emits OSC 8;;<uri>ST" {
        ansi(StartHyperlink("https://example.com")) shouldBe "${esc}]8;;https://example.com${esc}\\"
    }

    "StartHyperlink with params emits k=v separated by ':'" {
        val s = ansi(StartHyperlink("https://example.com", mapOf("id" to "42")))
        s shouldBe "${esc}]8;id=42;https://example.com${esc}\\"
    }

    "EndHyperlink emits OSC 8;;ST" {
        ansi(EndHyperlink) shouldBe "${esc}]8;;${esc}\\"
    }
})

package io.github.krossterm.integration

import io.github.krossterm.alternateScreen
import io.github.krossterm.execute
import io.github.krossterm.MoveTo
import io.github.krossterm.style.Print
import io.github.krossterm.synchronizedUpdate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringWriter

class DumbTerminalSpec : StringSpec({

    "Writer.alternateScreen wraps block in CSI ?1049h / l" {
        val w = StringWriter()
        w.alternateScreen {
            it.execute(MoveTo(0, 0), Print("hello"))
        }
        w.toString() shouldContain "[?1049h"
        w.toString() shouldContain "[1;1H"
        w.toString() shouldContain "hello"
        w.toString() shouldContain "[?1049l"
    }

    "Writer.alternateScreen restores main screen even when the block throws" {
        val w = StringWriter()
        var rethrown = false
        try {
            w.alternateScreen {
                it.execute(Print("about to fail"))
                error("simulated")
            }
        } catch (_: IllegalStateException) {
            rethrown = true
        }
        rethrown shouldBe true
        w.toString() shouldContain "[?1049h"
        w.toString() shouldContain "[?1049l"
    }

    "Writer.synchronizedUpdate brackets the block with DEC mode 2026" {
        val w = StringWriter()
        w.synchronizedUpdate {
            it.execute(Print("frame"))
        }
        w.toString() shouldContain "[?2026h"
        w.toString() shouldContain "frame"
        w.toString() shouldContain "[?2026l"
    }
})

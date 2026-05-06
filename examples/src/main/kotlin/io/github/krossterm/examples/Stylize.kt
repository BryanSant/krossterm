package io.github.krossterm.examples

import io.github.krossterm.style.Color
import io.github.krossterm.style.bold
import io.github.krossterm.style.crossedOut
import io.github.krossterm.style.darkGrey
import io.github.krossterm.style.green
import io.github.krossterm.style.italic
import io.github.krossterm.style.on
import io.github.krossterm.style.onYellow
import io.github.krossterm.style.red
import io.github.krossterm.style.underlined
import io.github.krossterm.style.with
import io.github.krossterm.style.yellow

/**
 * Showcase of the Stylize extension surface -- see how it composes by chaining.
 *
 * Run with: `./gradlew runExample -Pexample=Stylize`
 */
fun main() {
    println("--- named colors ---")
    println("error".red().bold())
    println("warning".yellow().italic())
    println("ok".green())
    println("note".darkGrey())

    println()
    println("--- backgrounds ---")
    println("on yellow".onYellow().bold())
    println("on red bg".on(Color.Red))

    println()
    println("--- truecolor ---")
    println("rgb 255,128,64".with(Color.Rgb(255, 128, 64)))
    println("256-palette idx 196".with(Color.AnsiValue(196)))

    println()
    println("--- attribute combos ---")
    println("bold + underlined + italic".bold().underlined().italic())
    println("crossed out".crossedOut())

    println()
    println("--- rgb spectrum ---")
    println((0 until 80).joinToString("") { i ->
        val (r, g, b) = hsvToRgb(i * 360f / 80f, 1f, 1f)
        "\u2588".with(Color.Rgb(r, g, b)).toString()
    })
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
    val hi = (h / 60f).toInt() % 6
    val f  = h / 60f - (h / 60f).toInt()
    val p  = (v * (1 - s)           * 255).toInt()
    val q  = (v * (1 - f * s)       * 255).toInt()
    val t  = (v * (1 - (1 - f) * s) * 255).toInt()
    val vi = (v * 255).toInt()
    return when (hi) {
        0    -> Triple(vi, t,  p)
        1    -> Triple(q,  vi, p)
        2    -> Triple(p,  vi, t)
        3    -> Triple(p,  q,  vi)
        4    -> Triple(t,  p,  vi)
        else -> Triple(vi, p,  q)
    }
}

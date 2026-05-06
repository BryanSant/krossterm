package io.github.krossterm.link

import io.github.krossterm.style.StyledContent

/**
 * Content wrapped in an OSC 8 clickable link.
 *
 * Obtain via [String.link] or [StyledContent.link]:
 * ```
 * println("Kotlin".link("https://kotlinlang.org"))
 * println("Kotlin".bold().cyan().link("https://kotlinlang.org"))
 * ```
 *
 * [toString] emits the full `OSC 8 … ST  content  OSC 8 ;; ST` sequence,
 * so instances can be passed directly to [print][io.github.krossterm.TerminalScope.print]
 * or used inside string templates.
 */
public data class LinkContent<out D>(
    val uri: String,
    val content: D,
    val params: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = buildString {
        StartLink(uri, params).writeAnsi(this)
        append(content.toString())
        EndLink.writeAnsi(this)
    }
}

/** Wrap this string in an OSC 8 clickable link. */
public fun String.link(
    uri: String,
    params: Map<String, String> = emptyMap(),
): LinkContent<String> = LinkContent(uri, this, params)

/**
 * Wrap this styled content in an OSC 8 clickable link.
 * Apply style extensions before calling [link] so the SGR codes sit
 * inside the OSC 8 boundaries.
 */
public fun <D> StyledContent<D>.link(
    uri: String,
    params: Map<String, String> = emptyMap(),
): LinkContent<StyledContent<D>> = LinkContent(uri, this, params)

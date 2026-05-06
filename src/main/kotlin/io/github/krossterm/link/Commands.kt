package io.github.krossterm.link

import io.github.krossterm.Command
import io.github.krossterm.ansi.Csi.OSC
import io.github.krossterm.ansi.Csi.ST

/**
 * Begin an OSC 8 clickable link. Text printed between this command and
 * [EndLink] becomes a link in supported terminals (iTerm2, Kitty, WezTerm,
 * GNOME Terminal, foot, etc.).
 *
 * @param uri target URI (e.g. `"https://example.com"`)
 * @param params optional `key=value` params recognized by the terminal — most
 *   commonly `id=...` to coalesce links spanning multiple lines.
 */
public data class StartLink(
    val uri: String,
    val params: Map<String, String> = emptyMap(),
) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("8;")
        var first = true
        for ((k, v) in params) {
            if (!first) out.append(':')
            out.append(k).append('=').append(v)
            first = false
        }
        out.append(';').append(uri).append(ST)
    }
}

/** End the most recently opened OSC 8 link. */
public data object EndLink : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(OSC).append("8;;").append(ST)
    }
}

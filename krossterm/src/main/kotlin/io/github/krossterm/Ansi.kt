package io.github.krossterm

/**
 * ANSI control-sequence string constants used to assemble escape sequences.
 *
 * Most commands emit a CSI sequence (`ESC [` … final byte). OSC sequences
 * (`ESC ]` … BEL or ST) are used for links (OSC 8) and clipboard (OSC 52).
 */
public object Ansi {
    /** ESC (`\u001B`). */
    public const val ESC: String = "\u001B"

    /** Control Sequence Introducer (`ESC [`). */
    public const val CSI: String = "\u001B["

    /** Single Shift 3 (`ESC O`) — function-key prefix used by some terminals. */
    public const val SS3: String = "\u001BO"

    /** Operating System Command introducer (`ESC ]`). */
    public const val OSC: String = "\u001B]"

    /** String Terminator (`ESC \`) — closes OSC and DCS sequences. */
    public const val ST: String = "\u001B\\"

    /** Bell (`\u0007`) — alternative OSC terminator. */
    public const val BEL: String = "\u0007"
}

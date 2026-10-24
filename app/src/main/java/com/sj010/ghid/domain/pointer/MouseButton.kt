package com.sj010.ghid.domain.pointer

import java.util.Locale

/**
 * Standard three-button mouse, using HID button usage bitmasks.
 *
 * The phone never fabricates a "double click" report: it emits two real
 * press/release cycles and lets the host OS decide, which behaves natively on
 * both Windows and macOS.
 */
enum class MouseButton(val mask: Byte) {
    LEFT(0x01),
    RIGHT(0x02),
    MIDDLE(0x04);

    companion object {
        /**
         * Resolves the token vocabulary shared with the Go CLI and combo
         * grammar. Returns null when [token] is not a button gesture.
         */
        fun fromToken(token: String): MouseButton? = when (token.trim().lowercase(Locale.ROOT)) {
            "click-left", "left-click", "leftclick", "lclick" -> LEFT
            "click-right", "right-click", "rightclick", "rclick" -> RIGHT
            "click-middle", "middle-click", "middleclick", "mclick" -> MIDDLE
            "double-click", "doubleclick", "dblclick" -> LEFT
            else -> null
        }
    }
}

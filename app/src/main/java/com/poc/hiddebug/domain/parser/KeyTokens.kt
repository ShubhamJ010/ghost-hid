package com.poc.hiddebug.domain.parser

import java.util.Locale

object KeyTokens {

    val MOUSE_ACTIONS = setOf(
        "click-left", "left-click", "leftclick", "lclick",
        "click-right", "right-click", "rightclick", "rclick",
        "click-middle", "middle-click", "middleclick", "mclick",
        "double-click", "doubleclick", "dblclick",
        "scroll-up", "scrollup", "wheel-up",
        "scroll-down", "scrolldown", "wheel-down"
    )

    fun isMouseAction(token: String): Boolean {
        return MOUSE_ACTIONS.contains(token.trim().lowercase(Locale.ROOT))
    }

    fun isModifier(token: String): Boolean {
        return when (token.trim().lowercase(Locale.ROOT)) {
            "win", "gui", "super", "ctrl", "control", "alt", "shift" -> true
            else -> false
        }
    }

    fun isMacAlias(token: String): Boolean {
        return when (token.trim().lowercase(Locale.ROOT)) {
            "cmd", "command", "option", "opt" -> true
            else -> false
        }
    }

    /**
     * Maps printable US-layout ASCII character to a HID modifier byte and keycode.
     * Returns Pair(modifier, keycode). Keycode 0 means unmapped.
     */
    fun keyForChar(c: Char): Pair<Byte, Byte> {
        var mod: Byte = 0x00
        var code: Byte = 0x00

        when (c) {
            in 'a'..'z' -> code = (0x04 + (c - 'a')).toByte()
            in 'A'..'Z' -> {
                mod = 0x02
                code = (0x04 + (c - 'A')).toByte()
            }
            in '1'..'9' -> code = (0x1E + (c - '1')).toByte()
            '0' -> code = 0x27
            ' ' -> code = 0x2C
            '\n', '\r' -> code = 0x28
            '\t' -> code = 0x2B
            '.' -> code = 0x37
            ',' -> code = 0x36
            '-' -> code = 0x2D
            '=' -> code = 0x2E
            ';' -> code = 0x33
            '\'' -> code = 0x34
            '[' -> code = 0x2F
            ']' -> code = 0x30
            '\\' -> code = 0x31
            '`' -> code = 0x35
            '/' -> code = 0x38
            '!' -> { mod = 0x02; code = 0x1E }
            '@' -> { mod = 0x02; code = 0x1F }
            '#' -> { mod = 0x02; code = 0x20 }
            '$' -> { mod = 0x02; code = 0x21 }
            '%' -> { mod = 0x02; code = 0x22 }
            '^' -> { mod = 0x02; code = 0x23 }
            '&' -> { mod = 0x02; code = 0x24 }
            '*' -> { mod = 0x02; code = 0x25 }
            '(' -> { mod = 0x02; code = 0x26 }
            ')' -> { mod = 0x02; code = 0x27 }
            '_' -> { mod = 0x02; code = 0x2D }
            '+' -> { mod = 0x02; code = 0x2E }
            '{' -> { mod = 0x02; code = 0x2F }
            '}' -> { mod = 0x02; code = 0x30 }
            '|' -> { mod = 0x02; code = 0x31 }
            ':' -> { mod = 0x02; code = 0x33 }
            '"' -> { mod = 0x02; code = 0x34 }
            '~' -> { mod = 0x02; code = 0x35 }
            '<' -> { mod = 0x02; code = 0x36 }
            '>' -> { mod = 0x02; code = 0x37 }
            '?' -> { mod = 0x02; code = 0x38 }
        }
        return Pair(mod, code)
    }

    /**
     * Parses a keyboard chord like "win+r", "ctrl+alt+del", "enter", "f5".
     */
    fun parseChord(chord: String): Pair<Byte, Byte> {
        val parts = chord.lowercase(Locale.ROOT).split("+")
        var mod: Byte = 0
        var key: Byte = 0

        for (rawPart in parts) {
            val p = rawPart.trim()
            when (p) {
                "win", "gui", "super", "meta" -> mod = (mod.toInt() or 0x08).toByte()
                "ctrl", "control" -> mod = (mod.toInt() or 0x01).toByte()
                "alt", "opt", "option" -> mod = (mod.toInt() or 0x04).toByte()
                "shift" -> mod = (mod.toInt() or 0x02).toByte()
                "del", "delete" -> key = 0x4C
                "backspace" -> key = 0x2A
                "esc", "escape" -> key = 0x29
                "enter", "return" -> key = 0x28
                "tab" -> key = 0x2B
                "space" -> key = 0x2C
                "up" -> key = 0x52
                "down" -> key = 0x51
                "left" -> key = 0x50
                "right" -> key = 0x4F
                "home" -> key = 0x4A
                "end" -> key = 0x4D
                "pageup", "pgup" -> key = 0x4B
                "pagedown", "pgdn" -> key = 0x4E
                "insert" -> key = 0x49
                "capslock", "caps" -> key = 0x39
                "numlock" -> key = 0x53
                "scrolllock", "scroll" -> key = 0x47
                "printscreen", "prtsc", "print" -> key = 0x46
                "pause", "break" -> key = 0x48
                "menu", "apps", "context" -> key = 0x65
                "minus" -> key = 0x2D
                "equal", "equals" -> key = 0x2E
                "leftbracket", "lbracket" -> key = 0x2F
                "rightbracket", "rbracket" -> key = 0x30
                "backslash" -> key = 0x31
                "semicolon" -> key = 0x33
                "quote", "apostrophe" -> key = 0x34
                "grave", "backtick", "tilde" -> key = 0x35
                "comma" -> key = 0x36
                "period", "dot" -> key = 0x37
                "slash", "forwardslash" -> key = 0x38
                else -> {
                    if (p.startsWith("f") && p.length in 2..3) {
                        val fNum = p.substring(1).toIntOrNull()
                        if (fNum != null && fNum in 1..12) {
                            key = (0x3A + (fNum - 1)).toByte()
                        }
                    } else if (p.length == 1) {
                        val c = p[0]
                        if (c in 'a'..'z') key = (0x04 + (c - 'a')).toByte()
                        else if (c in '1'..'9') key = (0x1E + (c - '1')).toByte()
                        else if (c == '0') key = 0x27
                    }
                }
            }
        }
        return Pair(mod, key)
    }
}

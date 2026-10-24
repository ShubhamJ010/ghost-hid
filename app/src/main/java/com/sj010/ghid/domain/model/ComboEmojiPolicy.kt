package com.sj010.ghid.domain.model

/**
 * Optional combo icon metadata.
 *
 * A combo's emoji overrides the target OS glyph in the app and the `[win]` /
 * `[mac]` badge in the CLI. The rules mirror `combos.ValidateEmoji` in Go so a
 * combo saved on the phone and one saved with `ghid combo save --emoji` agree.
 */
object ComboEmojiPolicy {

    const val MAX_CODE_POINTS = 12

    /** Trimmed emoji, or null when the combo keeps the OS icon. */
    fun normalize(value: String?): String? = value?.trim()?.ifEmpty { null }

    fun isValid(value: String?): Boolean {
        val clean = normalize(value) ?: return true
        if (clean.codePointCount(0, clean.length) > MAX_CODE_POINTS) return false
        return clean.none { char ->
            char.isWhitespace() || (char.code < 0x80 && (char.isLetter() || char.isDigit()))
        }
    }
}

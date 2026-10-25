package com.sj010.ghid.domain.parser

import com.sj010.ghid.domain.model.HidStep
import com.sj010.ghid.domain.model.TargetOs
import java.util.Locale

object ActionSequenceParser {

    /**
     * Parses a unified bracket sequence DSL string into a list of executable HidSteps.
     * Example: "samxn81@gmail.com{Tab}{PASSWORD}{Enter}{500ms}{mod+s}{100ms}{WIN_SLEEP}"
     */
    fun parse(raw: String, targetOs: TargetOs = TargetOs.WINDOWS): List<HidStep> {
        if (raw.isEmpty()) return emptyList()

        val steps = mutableListOf<HidStep>()
        val textBuf = StringBuilder()
        var i = 0

        fun flushText() {
            if (textBuf.isNotEmpty()) {
                steps.add(HidStep.TypeText(textBuf.toString()))
                textBuf.clear()
            }
        }

        while (i < raw.length) {
            val c = raw[i]
            if (c == '{') {
                if (i + 1 < raw.length && raw[i + 1] == '{') {
                    // Escaped literal '{'
                    textBuf.append('{')
                    i += 2
                    continue
                }

                // Find closing '}'
                val closeIdx = raw.indexOf('}', i + 1)
                if (closeIdx == -1) {
                    // Unclosed bracket, treat as literal text
                    textBuf.append(c)
                    i++
                    continue
                }

                flushText()
                val token = raw.substring(i + 1, closeIdx).trim()
                i = closeIdx + 1

                val step = parseToken(token, targetOs)
                if (step != null) {
                    steps.add(step)
                }
            } else {
                textBuf.append(c)
                i++
            }
        }

        flushText()
        return steps
    }

    private fun parseToken(token: String, targetOs: TargetOs): HidStep? {
        if (token.isEmpty()) return null

        var holdMs: Long? = null
        var tokenBody = token

        val colonIdx = token.lastIndexOf(':')
        if (colonIdx != -1) {
            val candidateToken = token.substring(0, colonIdx).trim()
            val candidateDur = token.substring(colonIdx + 1).trim().lowercase(Locale.ROOT)
            if (candidateToken.isNotEmpty() && (candidateDur.endsWith("ms") || candidateDur.endsWith("s"))) {
                val isMs = candidateDur.endsWith("ms")
                val numPart = if (isMs) candidateDur.removeSuffix("ms") else candidateDur.removeSuffix("s")
                val num = numPart.toDoubleOrNull()
                if (num != null && num > 0) {
                    holdMs = if (isMs) num.toLong() else (num * 1000.0).toLong()
                    tokenBody = candidateToken
                }
            }
        }

        // 1. Pure Delay: {500ms}, {1s}, {100ms} (when no hold modifier)
        if (holdMs == null) {
            val lower = tokenBody.lowercase(Locale.ROOT)
            if (lower.endsWith("ms") || lower.endsWith("s")) {
                val isMs = lower.endsWith("ms")
                val numPart = if (isMs) lower.removeSuffix("ms") else lower.removeSuffix("s")
                val num = numPart.toDoubleOrNull()
                if (num != null && num > 0) {
                    val millis = if (isMs) num.toLong() else (num * 1000.0).toLong()
                    return HidStep.Delay(millis)
                }
            }
        }

        // 2. Chords with '+' (e.g. mod+s, ctrl+alt+del)
        if (tokenBody.contains("+")) {
            val (mod, key) = KeyTokens.parseChord(tokenBody, targetOs)
            return HidStep.KeyboardChord(mod, key, tokenBody, durationMs = holdMs ?: 40L)
        }

        // 3. Consumer / Media Keys (e.g. VolumeUp, PlayPause, Mute)
        val consumer = KeyTokens.consumerKey(tokenBody)
        if (consumer != null) {
            val bytes = ByteArray(consumer.hex.length / 2) { idx ->
                consumer.hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
            return HidStep.MediaKey(consumer.name, consumer.hex, bytes, durationMs = holdMs ?: 60L)
        }

        // 4. Mouse Action (e.g. click-left, scroll-up)
        if (KeyTokens.isMouseAction(tokenBody)) {
            return HidStep.MouseAction(tokenBody, durationMs = holdMs)
        }

        // 5. Standard HID Key token (e.g. Tab, Enter, Escape, Del, F5, Backspace)
        if (KeyTokens.isStandardKey(tokenBody)) {
            val (mod, key) = KeyTokens.parseChord(tokenBody, targetOs)
            return HidStep.KeyboardChord(mod, key, tokenBody, durationMs = holdMs ?: 40L)
        }

        // 6. Direct Raw Report Hex (e.g. 02, cd00, 08000f0000000000)
        val cleanHex = tokenBody.replace(" ", "")
        if (cleanHex.length in listOf(2, 4, 8, 16) && cleanHex.all { it in "0123456789abcdefABCDEF" }) {
            val bytes = ByteArray(cleanHex.length / 2) { idx ->
                cleanHex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
            return HidStep.RawReport(bytes, cleanHex.lowercase(Locale.ROOT), durationMs = holdMs ?: 60L)
        }

        // 7. Named Payload Reference (e.g. {PASSWORD}, {EMAIL}, {WIN_SLEEP})
        if (tokenBody.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            return HidStep.PayloadReference(tokenBody, durationMs = holdMs)
        }

        return null
    }

    /**
     * Extracts all named payload references from a bracket sequence.
     */
    fun extractPayloads(sequence: String, targetOs: TargetOs = TargetOs.WINDOWS): List<String> {
        val steps = parse(sequence, targetOs)
        return steps.filterIsInstance<HidStep.PayloadReference>().map { it.name }.distinct()
    }

    /**
     * Validates that a sequence string is syntactically well-formed.
     */
    fun validate(sequence: String, targetOs: TargetOs = TargetOs.WINDOWS): Result<Unit> {
        if (sequence.isBlank()) {
            return Result.failure(IllegalArgumentException("Sequence cannot be empty"))
        }

        // Check for unclosed single brackets
        var inBracket = false
        var i = 0
        while (i < sequence.length) {
            val c = sequence[i]
            if (c == '{') {
                if (i + 1 < sequence.length && sequence[i + 1] == '{') {
                    i += 2
                    continue
                }
                if (inBracket) {
                    return Result.failure(IllegalArgumentException("Nested '{' is not allowed in sequence"))
                }
                inBracket = true
            } else if (c == '}') {
                if (!inBracket) {
                    return Result.failure(IllegalArgumentException("Unexpected '}' without opening '{'"))
                }
                inBracket = false
            }
            i++
        }
        if (inBracket) {
            return Result.failure(IllegalArgumentException("Unclosed '{' in sequence"))
        }

        val steps = parse(sequence, targetOs)
        if (steps.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid steps parsed"))
        }

        for (step in steps) {
            when (step) {
                is HidStep.KeyboardChord -> {
                    val parts = step.label.split("+").map { it.trim() }
                    if (step.label.endsWith("+") || parts.any { it.isEmpty() }) {
                        return Result.failure(IllegalArgumentException("Incomplete chord; specify a key after '+'"))
                    }
                    if (targetOs == TargetOs.WINDOWS || targetOs == TargetOs.LINUX) {
                        for (tok in parts) {
                            if (KeyTokens.isMacAlias(tok)) {
                                return Result.failure(IllegalArgumentException("\"$tok\" is a macOS key name; use 'mod' or a Windows/Linux key"))
                            }
                        }
                    }
                    if (step.keyCode == 0.toByte() && !parts.all { KeyTokens.isModifier(it) }) {
                        val unknown = parts.firstOrNull { !KeyTokens.isModifier(it) } ?: step.label
                        return Result.failure(IllegalArgumentException("Unknown key \"$unknown\""))
                    }
                }
                is HidStep.TypeText -> {
                    for (c in step.text) {
                        if (c.code < 0x20 || c.code > 0x7E) {
                            return Result.failure(IllegalArgumentException("Typed text supports printable ASCII only (found '$c')"))
                        }
                    }
                }
                else -> Unit
            }
        }

        return Result.success(Unit)
    }
}

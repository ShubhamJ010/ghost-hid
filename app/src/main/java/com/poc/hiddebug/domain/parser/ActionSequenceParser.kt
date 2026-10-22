package com.poc.hiddebug.domain.parser

import com.poc.hiddebug.domain.model.HidStep
import java.util.regex.Pattern

object ActionSequenceParser {

    private val DELAY_PATTERN = Pattern.compile("->\\{([0-9.]+)\\}")
    private val SIMPLE_ARROW_PATTERN = Pattern.compile("->")

    /**
     * Parses a raw combo string into a list of executable HidSteps.
     * Example: "win+r ->{0.5} type:cmd ->{0.3} enter"
     */
    fun parse(raw: String): List<HidStep> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        val steps = mutableListOf<HidStep>()

        // Replace custom delay arrow with a standard delimiter tag
        var normalized = trimmed
        val matcher = DELAY_PATTERN.matcher(trimmed)
        val delays = mutableListOf<Pair<Int, Long>>()

        // Tokenize by arrow boundaries
        val parts = normalized.split(SIMPLE_ARROW_PATTERN.toRegex())

        for (i in parts.indices) {
            var token = parts[i].trim()
            if (token.isEmpty()) continue

            var stepDelayMs = 0L

            // Extract delay if attached: e.g. "{0.5} step"
            if (token.startsWith("{") && token.contains("}")) {
                val closeIdx = token.indexOf('}')
                val secondsStr = token.substring(1, closeIdx)
                val seconds = secondsStr.toDoubleOrNull() ?: 0.0
                stepDelayMs = (seconds * 1000).toLong()
                token = token.substring(closeIdx + 1).trim()
            }

            if (stepDelayMs > 0) {
                steps.add(HidStep.Delay(stepDelayMs))
            }

            val step = parseSingleStep(token)
            if (step != null) {
                steps.add(step)
            }
        }

        return steps
    }

    private fun parseSingleStep(stepStr: String): HidStep? {
        val s = stepStr.trim()
        if (s.isEmpty()) return null

        // 1. Directives: !DISCONNECT, !EXIT
        if (s.startsWith("!")) {
            return HidStep.Directive(s.substring(1).trim().uppercase())
        }

        // 2. Variables: $USERNAME, ${PASSWORD}
        if (s.startsWith("$")) {
            val varName = s.removePrefix("\$" + "{").removePrefix("$").removeSuffix("}").trim()
            return HidStep.Variable(varName)
        }

        // 3. Text: type:hello world or text:hello
        if (s.startsWith("type:") || s.startsWith("text:")) {
            val colonIdx = s.indexOf(':')
            val text = s.substring(colonIdx + 1)
            return HidStep.TypeText(text)
        }

        // 4. Mouse: click-left, scroll-up
        if (KeyTokens.isMouseAction(s)) {
            return HidStep.MouseAction(s)
        }

        // 5. Keyboard Chord: win+l, ctrl+alt+del
        val (mod, key) = KeyTokens.parseChord(s)
        return HidStep.KeyboardChord(mod, key, s)
    }

    /**
     * Validates that a combo string is syntactically well-formed.
     */
    fun validate(keys: String): Result<Unit> {
        val trimmed = keys.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Combo has no steps"))

        val steps = parse(trimmed)
        if (steps.isEmpty()) return Result.failure(IllegalArgumentException("No valid steps parsed"))

        for (step in steps) {
            when (step) {
                is HidStep.KeyboardChord -> {
                    for (token in step.label.split("+")) {
                        val t = token.trim()
                        if (KeyTokens.isMacAlias(t)) {
                            return Result.failure(IllegalArgumentException("\"$t\" is a macOS key name; use a Windows key such as win, ctrl or alt"))
                        }
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

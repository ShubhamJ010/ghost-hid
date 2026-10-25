package com.sj010.ghid.domain.parser

import com.sj010.ghid.domain.model.HidStep

/**
 * Immutable snapshot of a raw sequence scheme after parsing and validation.
 *
 * This is the single source of truth for every sequence preview in the UI, so the
 * dashboard, quick-send composer and combo editor can never disagree about what a
 * scheme will do.
 */
data class SequenceAnalysis(
    val scheme: String,
    val steps: List<HidStep> = emptyList(),
    val error: String? = null,
    val totalDelayMillis: Long = 0L
) {
    val isBlank: Boolean get() = scheme.isBlank()
    val isInvalid: Boolean get() = error != null
    val canSend: Boolean get() = steps.isNotEmpty() && !isInvalid
}

object SequenceAnalyzer {

    fun analyze(scheme: String, targetOs: com.sj010.ghid.domain.model.TargetOs = com.sj010.ghid.domain.model.TargetOs.WINDOWS): SequenceAnalysis {
        val trimmed = scheme.trim()
        if (trimmed.isEmpty()) {
            return SequenceAnalysis(scheme = scheme)
        }

        ActionSequenceParser.validate(trimmed, targetOs).onFailure { failure ->
            return SequenceAnalysis(
                scheme = scheme,
                error = failure.message ?: "Invalid sequence"
            )
        }

        val steps = ActionSequenceParser.parse(trimmed)
        if (steps.isEmpty()) {
            return SequenceAnalysis(
                scheme = scheme,
                error = "No valid steps parsed"
            )
        }

        val totalTime = steps.sumOf { step ->
            when (step) {
                is HidStep.Delay -> step.millis
                is HidStep.KeyboardChord -> if (step.durationMs > 40L) step.durationMs else 0L
                is HidStep.RawReport -> if (step.durationMs != 60L) step.durationMs else 0L
                is HidStep.PayloadReference -> step.durationMs ?: 0L
                else -> 0L
            }
        }

        return SequenceAnalysis(
            scheme = scheme,
            steps = steps,
            totalDelayMillis = totalTime
        )
    }
}

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

    fun analyze(scheme: String): SequenceAnalysis {
        val trimmed = scheme.trim()
        if (trimmed.isEmpty()) {
            return SequenceAnalysis(scheme = scheme)
        }

        ActionSequenceParser.validate(trimmed).onFailure { failure ->
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

        return SequenceAnalysis(
            scheme = scheme,
            steps = steps,
            totalDelayMillis = steps.filterIsInstance<HidStep.Delay>().sumOf { it.millis }
        )
    }
}

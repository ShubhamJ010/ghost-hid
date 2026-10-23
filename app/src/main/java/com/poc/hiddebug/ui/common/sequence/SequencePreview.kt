package com.poc.hiddebug.ui.common.sequence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.hiddebug.domain.model.HidStep
import com.poc.hiddebug.domain.parser.SequenceAnalysis
import com.poc.hiddebug.ui.theme.MaterialShapes
import java.util.Locale

fun formatSequenceDuration(millis: Long): String {
    if (millis <= 0L) return "instant"
    return String.format(Locale.US, "~%.1fs", millis / 1000.0)
}

fun formatStepCount(count: Int): String =
    if (count == 1) "1 step" else "$count steps"

/**
 * The one shared live preview for every sequence producer: quick send, combo cards and
 * the combo editor. Parsing is done by the caller once and passed in, so the chips and
 * the validation badge always describe the same analysis.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SequencePreview(
    analysis: SequenceAnalysis,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(MaterialShapes.ShapeClover4)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "Sequence preview",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                PreviewStatusPill(analysis)
            }

            when {
                analysis.isInvalid -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = analysis.error ?: "Invalid sequence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                analysis.steps.isEmpty() -> {
                    Text(
                        text = "Start typing a sequence and every step appears here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                compact -> {
                    SequenceStepChips(
                        steps = analysis.steps,
                        maxItems = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 148.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            analysis.steps.forEach { StepBadge(step = it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewStatusPill(analysis: SequenceAnalysis) {
    val label: String
    val background: Color
    val content: Color
    when {
        analysis.isInvalid -> {
            label = "Needs a fix"
            background = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
        analysis.steps.isEmpty() -> {
            label = "Waiting"
            background = MaterialTheme.colorScheme.surfaceContainerHighest
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            label = "${formatStepCount(analysis.steps.size)} · ${formatSequenceDuration(analysis.totalDelayMillis)}"
            background = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
        }
    }

    Surface(shape = CircleShape, color = background) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StepBadge(step: HidStep) {
    val icon = when (step) {
        is HidStep.KeyboardChord -> Icons.Default.Keyboard
        is HidStep.TypeText -> Icons.Default.TextFields
        is HidStep.Variable -> Icons.Default.Key
        is HidStep.Delay -> Icons.Default.Timer
        is HidStep.MouseAction -> Icons.Default.Mouse
        is HidStep.Directive -> Icons.Default.Warning
    }
    val label = when (step) {
        is HidStep.KeyboardChord -> step.label
        is HidStep.TypeText -> "\"${step.text}\""
        is HidStep.Variable -> "$${step.name}"
        is HidStep.Delay -> String.format(Locale.US, "%.1fs", step.millis / 1000.0)
        is HidStep.MouseAction -> step.action
        is HidStep.Directive -> "!${step.name}"
    }
    val background = when (step) {
        is HidStep.KeyboardChord -> MaterialTheme.colorScheme.primaryContainer
        is HidStep.TypeText -> MaterialTheme.colorScheme.secondaryContainer
        is HidStep.Variable -> MaterialTheme.colorScheme.tertiaryContainer
        is HidStep.Delay -> MaterialTheme.colorScheme.surfaceContainerHighest
        is HidStep.MouseAction -> MaterialTheme.colorScheme.primaryContainer
        is HidStep.Directive -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (step) {
        is HidStep.KeyboardChord -> MaterialTheme.colorScheme.onPrimaryContainer
        is HidStep.TypeText -> MaterialTheme.colorScheme.onSecondaryContainer
        is HidStep.Variable -> MaterialTheme.colorScheme.onTertiaryContainer
        is HidStep.Delay -> MaterialTheme.colorScheme.onSurfaceVariant
        is HidStep.MouseAction -> MaterialTheme.colorScheme.onPrimaryContainer
        is HidStep.Directive -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(shape = RoundedCornerShape(9.dp), color = background) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}

/** Compact chip summary used by history cards and other dense surfaces. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SequenceStepChips(
    steps: List<HidStep>,
    modifier: Modifier = Modifier,
    maxItems: Int = 4
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        steps.take(maxItems).forEach { StepBadge(step = it) }
        if (steps.size > maxItems) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "+${steps.size - maxItems}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

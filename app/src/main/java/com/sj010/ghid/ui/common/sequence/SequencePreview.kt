package com.sj010.ghid.ui.common.sequence

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sj010.ghid.domain.model.HidStep
import com.sj010.ghid.domain.parser.SequenceAnalysis
import com.sj010.ghid.ui.theme.MaterialShapes
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    showHeader: Boolean = true,
    showStatusPill: Boolean = true,
    onLongPress: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null) {
                    Modifier.pointerInput(analysis) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showHeader) {
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
                    if (showStatusPill) {
                        PreviewStatusPill(analysis)
                    }
                }
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

/**
 * Full-preview popup opened by long-pressing a sequence preview. Sized to its content
 * so every step is readable, with the step count, total time, and an optional edit action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SequencePreviewDialog(
    analysis: SequenceAnalysis,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(MaterialShapes.ShapeClover4)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sequence preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DialogStatPill(
                                    text = if (analysis.steps.isEmpty()) {
                                        "0 steps"
                                    } else {
                                        formatStepCount(analysis.steps.size)
                                    },
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DialogStatPill(
                                    text = formatSequenceDuration(analysis.totalDelayMillis),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    when {
                        analysis.isInvalid -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
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
                                text = "No steps in this sequence yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
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

                    if (onEdit != null) {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onEdit()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit combo",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogStatPill(text: String, color: Color, contentColor: Color) {
    Surface(shape = CircleShape, color = color) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
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
        is HidStep.PayloadReference -> Icons.Default.Key
        is HidStep.Delay -> Icons.Default.Timer
        is HidStep.MouseAction -> Icons.Default.Mouse
        is HidStep.RawReport -> Icons.Default.Bolt
    }
    val label = when (step) {
        is HidStep.KeyboardChord -> if (step.durationMs > 40L) "${step.label}:${step.durationMs}ms" else step.label
        is HidStep.TypeText -> "\"${step.text}\""
        is HidStep.PayloadReference -> if (step.durationMs != null && step.durationMs > 0) "{${step.name}:${step.durationMs}ms}" else "{${step.name}}"
        is HidStep.Delay -> if (step.millis >= 1000 && step.millis % 1000 == 0L) "${step.millis / 1000}s" else "${step.millis}ms"
        is HidStep.MouseAction -> if (step.durationMs != null && step.durationMs > 0) "${step.action}:${step.durationMs}ms" else step.action
        is HidStep.RawReport -> if (step.durationMs != 60L) "${step.hex}:${step.durationMs}ms" else step.hex
    }
    val background = when (step) {
        is HidStep.KeyboardChord -> MaterialTheme.colorScheme.primaryContainer
        is HidStep.TypeText -> MaterialTheme.colorScheme.secondaryContainer
        is HidStep.PayloadReference -> MaterialTheme.colorScheme.tertiaryContainer
        is HidStep.Delay -> MaterialTheme.colorScheme.surfaceContainerHighest
        is HidStep.MouseAction -> MaterialTheme.colorScheme.primaryContainer
        is HidStep.RawReport -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (step) {
        is HidStep.KeyboardChord -> MaterialTheme.colorScheme.onPrimaryContainer
        is HidStep.TypeText -> MaterialTheme.colorScheme.onSecondaryContainer
        is HidStep.PayloadReference -> MaterialTheme.colorScheme.onTertiaryContainer
        is HidStep.Delay -> MaterialTheme.colorScheme.onSurfaceVariant
        is HidStep.MouseAction -> MaterialTheme.colorScheme.onPrimaryContainer
        is HidStep.RawReport -> MaterialTheme.colorScheme.onTertiaryContainer
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

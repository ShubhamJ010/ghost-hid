package com.sj010.ghid.ui.streamdeck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Payload
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.theme.ExpressiveShapes
import java.util.Locale

/**
 * Payloads inside the landscape side drawer. Pills are sorted by dispatch count
 * (most-used first), sized from the payload name and laid out from the right
 * edge leftwards, each with a stable Material 3 Expressive silhouette.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamDeckPayloadShade(
    payloads: List<Payload>,
    useCounts: Map<String, Int>,
    targetOs: TargetOs,
    onSend: (Payload) -> Unit,
    onClose: () -> Unit
) {
    val sortedPayloads = remember(payloads, useCounts) { sortPayloads(payloads, useCounts) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HostChip(targetOs = targetOs)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClose()
                },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                tonalElevation = 1.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close payload drawer",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sortedPayloads.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(ExpressiveShapes.Puffy)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "No payloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create payloads in the Payload studio and they will\nappear here as ready-to-send pills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Right-to-left flow: the most-used pill sits at the right edge and
            // each following pill fills towards the left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sortedPayloads.forEach { payload ->
                        PayloadPill(
                            payload = payload,
                            useCount = useCounts[payload.name.trim().lowercase(Locale.ROOT)] ?: 0,
                            onClick = { onSend(payload) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HostChip(targetOs: TargetOs) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHighest.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (targetOs.isKnown) {
                OsGlyph(
                    os = targetOs,
                    tint = scheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = targetOs.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No host",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PayloadPill(
    payload: Payload,
    useCount: Int,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = remember(payload.name) { pillShape(payload.name) }
    val scheme = MaterialTheme.colorScheme
    val palette = remember(payload.name, scheme) { pillPalette(payload.name, scheme) }
    val length = payload.name.length.coerceAtMost(22)
    val width = (92 + length * 5.6f).dp.coerceIn(100.dp, 214.dp)
    val height = (50 + length / 6 * 2).dp.coerceIn(50.dp, 58.dp)

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .width(width)
            .height(height),
        shape = shape,
        color = palette.container,
        contentColor = palette.content,
        border = BorderStroke(1.dp, palette.content.copy(alpha = 0.28f)),
        tonalElevation = 1.dp
    ) {
        // The drawer flows right-to-left; pill content stays left-to-right.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        payload.secure -> Icons.Default.Lock
                        payload.isRaw -> Icons.Default.Memory
                        else -> Icons.Default.TextFields
                    },
                    contentDescription = null,
                    tint = palette.content.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = payload.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (useCount > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(palette.content.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = useCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = palette.content
                        )
                    }
                }
            }
        }
    }
}

private data class PillPalette(
    val container: Color,
    val content: Color
)

private fun pillPalette(name: String, scheme: ColorScheme): PillPalette {
    return when (stableHash(name) % 6) {
        0 -> PillPalette(scheme.primaryContainer, scheme.onPrimaryContainer)
        1 -> PillPalette(scheme.secondaryContainer, scheme.onSecondaryContainer)
        2 -> PillPalette(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        3 -> PillPalette(scheme.surfaceContainerHighest, scheme.onSurface)
        4 -> PillPalette(scheme.surfaceContainerHigh, scheme.onSurface)
        else -> PillPalette(scheme.primary, scheme.onPrimary)
    }
}

/**
 * Expressive silhouettes that keep their identity when stretched into a wide
 * tag. Spiky or point-balanced shapes (cookies, bursts, diamonds, gem) are left
 * out on purpose: stretched to a pill they read as a kinked blob.
 */
private val payloadPillShapes: List<Shape> by lazy {
    listOf(
        ExpressiveShapes.Pill,
        ExpressiveShapes.Oval,
        ExpressiveShapes.SemiCircle,
        ExpressiveShapes.Square,
        ExpressiveShapes.Slanted,
        ExpressiveShapes.Arch,
        ExpressiveShapes.Bun,
        ExpressiveShapes.Puffy
    )
}

private fun pillShape(name: String): Shape =
    payloadPillShapes[stableHash(name) % payloadPillShapes.size]

private fun stableHash(name: String): Int = name.hashCode() and Int.MAX_VALUE

private fun sortPayloads(payloads: List<Payload>, useCounts: Map<String, Int>): List<Payload> {
    return payloads.sortedWith(
        compareByDescending<Payload> {
            useCounts[it.name.trim().lowercase(Locale.ROOT)] ?: 0
        }.thenBy { it.name.lowercase(Locale.ROOT) }
    )
}

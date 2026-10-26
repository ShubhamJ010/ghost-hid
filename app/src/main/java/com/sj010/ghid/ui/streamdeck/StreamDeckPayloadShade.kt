package com.sj010.ghid.ui.streamdeck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Payload
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.theme.MaterialShapes
import java.util.Locale

/**
 * Morphing dock shared by the closed stream deck and the expanded payload shade.
 * Closed it is a quiet title strip; open it grows the payload count and close
 * controls exactly like the payload/gesture shades elsewhere in the app.
 */
@Composable
fun StreamDeckDock(
    progress: Float,
    payloadCount: Int,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isClosed = progress < 0.15f
    val isOpen = progress > 0.85f

    val sideProgress = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
    val barOffsetY = lerp(28.dp, 4.dp, progress)
    val textOffsetY = lerp(4.dp, 16.dp, progress)
    val closedTextAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
    val openTextAlpha = ((progress - 0.4f) * 2.5f).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                enabled = isClosed,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpen()
                }
            ),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // LEFT: payload count badge blossoms in with the open shade.
            if (progress > 0.08f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .graphicsLayer {
                            alpha = sideProgress
                            scaleX = 0.3f + 0.7f * sideProgress
                            scaleY = 0.3f + 0.7f * sideProgress
                        },
                    shape = MaterialShapes.ShapeDiamond,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    tonalElevation = 3.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = payloadCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // CENTER: drag handle slides above the title as the shade opens.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(210.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = barOffsetY)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                )

                Box(
                    modifier = Modifier.offset(y = textOffsetY),
                    contentAlignment = Alignment.Center
                ) {
                    if (closedTextAlpha > 0f) {
                        Text(
                            text = "stream deck",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.graphicsLayer { alpha = closedTextAlpha }
                        )
                    }
                    if (openTextAlpha > 0f) {
                        Text(
                            text = "Payloads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer { alpha = openTextAlpha }
                        )
                    }
                }
            }

            // RIGHT: close control.
            if (progress > 0.08f) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    enabled = isOpen,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .graphicsLayer {
                            alpha = sideProgress
                            scaleX = 0.3f + 0.7f * sideProgress
                            scaleY = 0.3f + 0.7f * sideProgress
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            contentDescription = "Close payload shade",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Payload cloud inside the open shade. Pills are auto-sorted by dispatch count
 * (most-used first, then alphabetically), sized from the payload name and given
 * a stable random silhouette from the M3 Expressive catalogue.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamDeckPayloadShade(
    payloads: List<Payload>,
    useCounts: Map<String, Int>,
    targetOs: TargetOs,
    onSend: (Payload) -> Unit
) {
    val sortedPayloads = remember(payloads, useCounts) { sortPayloads(payloads, useCounts) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(MaterialShapes.ShapeClover4)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payloads",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Most used first · tap a pill to send",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            HostChip(targetOs = targetOs)
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
                            .clip(MaterialShapes.ShapePuffy)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Keep the last pills clear of the morphing dock at the bottom.
        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun HostChip(targetOs: TargetOs) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.8f),
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
    val length = payload.name.length.coerceAtMost(24)
    val width = (86 + length * 6.2f).dp.coerceIn(100.dp, 238.dp)
    val height = (50 + length / 5 * 2).dp.coerceIn(50.dp, 60.dp)

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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 17.dp),
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
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = payload.name,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = palette.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (useCount > 1) {
                Spacer(modifier = Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .size(21.dp)
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

private val payloadPillShapes: List<Shape> by lazy {
    listOf(
        MaterialShapes.ShapePill,
        MaterialShapes.ShapeClamShell,
        MaterialShapes.ShapeArch,
        MaterialShapes.ShapeOval,
        MaterialShapes.ShapeSemiCircle,
        MaterialShapes.ShapePuffy,
        MaterialShapes.ShapePuffyDiamond,
        MaterialShapes.ShapeCookie4,
        MaterialShapes.ShapeCookie6,
        MaterialShapes.ShapeCookie7,
        MaterialShapes.ShapeCookie9,
        MaterialShapes.ShapeBun,
        MaterialShapes.ShapeGem,
        MaterialShapes.ShapeFan,
        MaterialShapes.ShapeSlanted,
        MaterialShapes.ShapeSquare,
        MaterialShapes.ShapeCircle,
        MaterialShapes.ShapeDiamond,
        MaterialShapes.ShapePentagon,
        MaterialShapes.ShapeSoftBurst,
        MaterialShapes.ShapeFlower,
        MaterialShapes.ShapeClover4
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

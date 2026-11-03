package com.sj010.ghid.ui.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.Payload
import com.sj010.ghid.service.HidService
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.model.UsageTrackingScope
import java.util.Locale

// PixelPlayer-inspired layout constants adapted for HID payloads
const val PayloadPillsPerColumn = 3
val PayloadPillHeight = 58.dp
val PayloadPillArtSize = 38.dp
val PayloadPillSpacing = 8.dp
val PayloadWidthSteps = listOf(148.dp, 166.dp, 184.dp, 202.dp, 220.dp)

/**
 * PixelPlayer-style 3-row staggered horizontal pill carousel for most used payloads.
 *
 * Sits directly below the Most Used Combos section on the Dashboard screen.
 * Implements a clean-room Material 3 Expressive capsule grid with dynamic step
 * widths, tactile spring morph feedback on press, and direct one-tap HID dispatch.
 */
@Composable
fun MostUsedPayloadsSection(
    payloads: List<Payload>,
    useCounts: Map<String, Int>,
    connectedOs: TargetOs = TargetOs.UNKNOWN,
    deviceName: String? = null,
    scope: UsageTrackingScope = UsageTrackingScope.DEVICE_TYPE,
    onRunPayload: (Payload) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayPayloads = remember(payloads, useCounts) {
        PayloadPillsPolicy.resolveDisplayPayloads(payloads, useCounts)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // Section Header: title + subtitle
        MostUsedPayloadsHeader(
            connectedOs = connectedOs,
            deviceName = deviceName,
            scope = scope
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (displayPayloads.isEmpty()) {
            MostUsedPayloadsEmptyState()
        } else {
            val pillRows = remember(displayPayloads) {
                PayloadPillsPolicy.buildPillRows(displayPayloads)
            }
            val scrollState = rememberScrollState()

            // Horizontally scrolling 3-row grid with bleed to screen edges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val horizontalBleed = 16.dp.roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxWidth = constraints.maxWidth + horizontalBleed * 2
                            )
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(-horizontalBleed, 0)
                        }
                    }
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(PayloadPillSpacing)
                ) {
                    pillRows.forEachIndexed { rowIndex, rowPayloads ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PayloadPillSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shift the whole course so the first column is not one vertical edge.
                            val startOffset = PayloadPillsPolicy.rowStartOffset(rowIndex)
                            if (startOffset > 0.dp) {
                                Spacer(modifier = Modifier.width(startOffset))
                            }
                            rowPayloads.forEachIndexed { colIndex, payload ->
                                val globalIndex = rowIndex + colIndex * PayloadPillsPerColumn
                                PayloadPillItem(
                                    payload = payload,
                                    index = globalIndex,
                                    rowIndex = rowIndex,
                                    colIndex = colIndex,
                                    onClick = { onRunPayload(payload) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section header with title and scope subtitle.
 */
@Composable
private fun MostUsedPayloadsHeader(
    connectedOs: TargetOs,
    deviceName: String?,
    scope: UsageTrackingScope
) {
    val subtitleText = when {
        scope == UsageTrackingScope.DEVICE_NAME && !deviceName.isNullOrBlank() -> {
            "Based on $deviceName history"
        }
        connectedOs.isKnown -> {
            "Based on ${connectedOs.label} history"
        }
        else -> {
            "Tap to dispatch keystrokes & raw reports"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "Most Used Payloads",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Single capsule pill representing a payload.
 *
 * Features:
 * - Dynamic stepped width based on content length for staggered interlocking columns
 * - Tactile corner radius spring morph on press (29dp stadium down to 18dp squarish pill)
 * - Harmonious M3 Expressive container and art badge colors
 * - Payload type icon (Lock for secret, Memory for raw hex, TextFields for keystroke)
 * - Name and masked value / descriptor subtitle
 */
@Composable
private fun PayloadPillItem(
    payload: Payload,
    index: Int,
    rowIndex: Int = 0,
    colIndex: Int = 0,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val subtitle = remember(payload) {
        when {
            payload.secure -> "••••••••"
            payload.isRaw -> "Raw Report"
            else -> payload.value?.takeIf { it.isNotBlank() } ?: "Keystrokes"
        }
    }
    val width = remember(payload.name, subtitle, rowIndex, colIndex) {
        PayloadPillsPolicy.calculatePillWidth(
            title = payload.name,
            subtitle = subtitle,
            rowIndex = rowIndex,
            colIndex = colIndex
        )
    }
    val palette = rememberPayloadPalette(name = payload.name, index = index)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 18.dp else 29.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "payloadPillCornerRadius"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(cornerRadius),
        color = palette.container,
        contentColor = palette.content,
        interactionSource = interactionSource,
        modifier = Modifier
            .width(width)
            .height(PayloadPillHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar Badge (38dp)
            Box(
                modifier = Modifier
                    .size(PayloadPillArtSize)
                    .clip(CircleShape)
                    .background(palette.artContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        payload.secure -> Icons.Default.Lock
                        payload.isRaw -> Icons.Default.Memory
                        else -> Icons.Default.TextFields
                    },
                    contentDescription = null,
                    tint = palette.content,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = payload.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = palette.content.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Color palette for a pill card and its circular leading avatar.
 */
data class PayloadPillPalette(
    val container: Color,
    val content: Color,
    val artContainer: Color
)

@Composable
fun rememberPayloadPalette(name: String, index: Int): PayloadPillPalette {
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    return remember(name, index, isDark, scheme) {
        getPayloadPalette(index = index, isDark = isDark, scheme = scheme)
    }
}

/**
 * Curated Material 3 Expressive tonal palettes matching the vibrant staggered pill style.
 */
fun getPayloadPalette(index: Int, isDark: Boolean, scheme: ColorScheme): PayloadPillPalette {
    return if (isDark) {
        when (index % 8) {
            0 -> PayloadPillPalette(Color(0xFF4A2518), Color(0xFFFFDBD0), Color(0x33FFDBD0)) // Warm Terracotta / Rust
            1 -> PayloadPillPalette(Color(0xFF2B2850), Color(0xFFE0E0FF), Color(0x33E0E0FF)) // Indigo / Lavender
            2 -> PayloadPillPalette(Color(0xFF43360B), Color(0xFFFFDF9B), Color(0x33FFDF9B)) // Amber / Goldenrod
            3 -> PayloadPillPalette(Color(0xFF143738), Color(0xFF80F5F6), Color(0x3380F5F6)) // Deep Teal / Cyan
            4 -> PayloadPillPalette(Color(0xFF3E2140), Color(0xFFF9D8FC), Color(0x33F9D8FC)) // Mulberry / Plum
            5 -> PayloadPillPalette(Color(0xFF1B3824), Color(0xFFA2F3BC), Color(0x33A2F3BC)) // Sage / Emerald
            6 -> PayloadPillPalette(Color(0xFF4E1D22), Color(0xFFFFDADA), Color(0x33FFDADA)) // Crimson / Rose
            else -> PayloadPillPalette(Color(0xFF1B3047), Color(0xFFD4E3FF), Color(0x33D4E3FF)) // Slate Blue / Navy
        }
    } else {
        when (index % 8) {
            0 -> PayloadPillPalette(Color(0xFFFFDAD4), Color(0xFF3E0A06), Color(0x2B3E0A06))
            1 -> PayloadPillPalette(Color(0xFFE0E0FF), Color(0xFF14133B), Color(0x2B14133B))
            2 -> PayloadPillPalette(Color(0xFFFFDF9E), Color(0xFF261A00), Color(0x2B261A00))
            3 -> PayloadPillPalette(Color(0xFFBCEDEE), Color(0xFF002021), Color(0x2B002021))
            4 -> PayloadPillPalette(Color(0xFFF7D9FA), Color(0xFF2A102C), Color(0x2B2A102C))
            5 -> PayloadPillPalette(Color(0xFFBAF3CE), Color(0xFF002110), Color(0x2B002110))
            6 -> PayloadPillPalette(Color(0xFFFFDADA), Color(0xFF41000A), Color(0x2B41000A))
            else -> PayloadPillPalette(Color(0xFFD2E4FF), Color(0xFF001D36), Color(0x2B001D36))
        }
    }
}

/**
 * Fallback empty state when no payloads exist.
 */
@Composable
private fun MostUsedPayloadsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "No payloads yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Add snippets or quick actions to see them here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Helper object for sorting, partitioning into 3 rows, and computing stepped widths.
 */
object PayloadPillsPolicy {

    fun resolveDisplayPayloads(
        payloads: List<Payload>,
        useCounts: Map<String, Int>,
        maxItems: Int = 15
    ): List<Payload> {
        if (payloads.isEmpty()) return emptyList()

        return payloads.sortedWith(
            compareByDescending<Payload> {
                useCounts[it.name.trim().lowercase(Locale.ROOT)] ?: 0
            }.thenBy { it.name.lowercase(Locale.ROOT) }
        ).take(maxItems)
    }

    fun buildPillRows(payloads: List<Payload>): List<List<Payload>> {
        val total = payloads.size
        if (total == 0) return emptyList()
        val rowCount = minOf(PayloadPillsPerColumn, total)
        val base = total / rowCount
        val remainder = total % rowCount
        val targetCounts = IntArray(rowCount) { index ->
            base + if (index < remainder) 1 else 0
        }

        val rows = List(rowCount) { mutableListOf<Payload>() }
        var itemIndex = 0
        while (itemIndex < total) {
            for (rowIndex in 0 until rowCount) {
                if (rows[rowIndex].size < targetCounts[rowIndex] && itemIndex < total) {
                    rows[rowIndex].add(payloads[itemIndex++])
                }
            }
        }
        return rows
    }

    // Stagger rhythm pattern offsets in dp for (rowIndex, colIndex % 4):
    // Creates the signature PixelPlayer interlocking wavy brickwork pattern where
    // pills in adjacent rows never align vertically and have irregular lengths.
    private val STAGGER_OFFSETS = arrayOf(
        intArrayOf(-16, 28, -10, 20),   // Row 0: compact, wide, compact, medium-wide
        intArrayOf(22, -14, 24, -12),  // Row 1: medium-wide, compact, wide, compact
        intArrayOf(-8, 36, -18, 22)    // Row 2: medium-compact, extra-wide, compact, wide
    )

    // Leading inset per row, in dp. The width stagger only moves the trailing edge,
    // which left the first column as one straight start line. These insets move
    // that same irregular rhythm onto the leading edge. No two rows share a start.
    private val ROW_START_OFFSETS = floatArrayOf(0f, 44f, 18f)

    fun rowStartOffset(rowIndex: Int): Dp {
        if (ROW_START_OFFSETS.isEmpty()) return 0.dp
        return ROW_START_OFFSETS[rowIndex.coerceIn(0, ROW_START_OFFSETS.lastIndex)].dp
    }

    fun calculatePillWidth(
        title: String,
        subtitle: String?,
        rowIndex: Int = 0,
        colIndex: Int = 0
    ): Dp {
        val effectiveLen = title.length * 1.15f + (subtitle?.length ?: 0) * 0.35f
        val contentBase = when {
            effectiveLen < 8f -> 130f
            effectiveLen < 14f -> 146f
            effectiveLen < 22f -> 168f
            effectiveLen < 30f -> 190f
            effectiveLen < 40f -> 212f
            else -> 232f
        }

        val rowPattern = STAGGER_OFFSETS[rowIndex.coerceIn(0, 2)]
        val offset = rowPattern[colIndex % rowPattern.size]

        // Ensure pill widths remain between 118.dp and 252.dp for optimal layout
        val finalWidth = (contentBase + offset).coerceIn(118f, 252f)
        return finalWidth.dp
    }
}

/**
 * Dispatcher helper to send raw reports or keystrokes to the target host and record usage.
 */
object PayloadDispatcher {

    fun dispatch(service: HidService?, payload: Payload) {
        val sender = service ?: return
        val value = payload.value?.trim().orEmpty()
        if (value.isEmpty()) return

        val targetOs = sender.effectiveTargetOs.value
        val deviceName = sender.targetDeviceState.value?.name

        val explicitRaw = value.startsWith("raw:", ignoreCase = true)
        if (payload.isRaw || explicitRaw) {
            val hex = if (explicitRaw) value.substring(4).trim() else value
            val cleanHex = hex.replace(" ", "")
            val validHex = cleanHex.length in listOf(2, 4, 8, 16) &&
                cleanHex.all { it in "0123456789abcdefABCDEF" }
            if (!validHex) return

            GhostHidApp.instance.payloadUsageRepository.recordUse(
                name = payload.name,
                os = targetOs,
                deviceName = deviceName
            )
            val bytes = ByteArray(cleanHex.length / 2) { index ->
                cleanHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
            sender.sendRawReport(bytes)
        } else {
            GhostHidApp.instance.payloadUsageRepository.recordUse(
                name = payload.name,
                os = targetOs,
                deviceName = deviceName
            )
            sender.typeText(value)
        }
    }
}

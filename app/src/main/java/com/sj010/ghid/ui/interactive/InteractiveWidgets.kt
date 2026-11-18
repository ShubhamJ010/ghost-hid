package com.sj010.ghid.ui.interactive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sj010.ghid.domain.model.AppSettings
import com.sj010.ghid.domain.model.TargetOs


sealed interface PreviewItem {
    val timestamp: Long

    data class TextChunk(
        val text: String,
        override val timestamp: Long
    ) : PreviewItem

    data class KeyPress(
        val label: String,
        val modifierPrefix: String? = null,
        val count: Int = 1,
        override val timestamp: Long
    ) : PreviewItem
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InteractivePreviewChips(
    heldModifiers: Set<InteractiveModifier>,
    history: List<PreviewItem>,
    targetOs: TargetOs,
    modifier: Modifier = Modifier
) {
    if (heldModifiers.isEmpty() && history.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        heldModifiers.forEach { held ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = held.labelFor(targetOs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        history.forEach { item ->
            when (item) {
                is PreviewItem.TextChunk -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is PreviewItem.KeyPress -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (item.modifierPrefix != null) {
                                Text(
                                    text = "${item.modifierPrefix} +",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (item.label.isNotBlank()) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.count > 1) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "x${item.count}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierKeyChip(
    modifier: Modifier,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 8.dp,
            bottomEnd = 14.dp,
            bottomStart = 8.dp
        ),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Keyboard island deck with modifiers, 4 mapper keys, and center-aligned touch instruction.
 */
@Composable
internal fun KeyboardIsland(
    detectedOs: TargetOs,
    heldModifiers: Set<InteractiveModifier>,
    mapperKeys: List<String>,
    onToggleModifier: (InteractiveModifier) -> Unit,
    onKeyClick: (InteractiveKey) -> Unit,
    onSlotEdit: (Int) -> Unit,
    onIslandClick: () -> Unit,
    hint: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIslandClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Modifier keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                InteractiveKeys.modifiers.forEach { modifier ->
                    ModifierKeyChip(
                        modifier = Modifier.weight(1f),
                        label = modifier.labelFor(detectedOs),
                        active = modifier in heldModifiers,
                        onClick = { onToggleModifier(modifier) }
                    )
                }
            }

            // Row 2: 4 mapper buttons (defaults: Esc, Tab, Stab, dotted +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0 until AppSettings.MAPPER_SLOTS).forEach { slotIndex ->
                    val key = InteractiveKeys.findByToken(mapperKeys.getOrElse(slotIndex) { "" })
                    if (key != null) {
                        MapperKeyButton(
                            modifier = Modifier.weight(1f),
                            key = key,
                            onClick = { onKeyClick(key) },
                            onLongClick = { onSlotEdit(slotIndex) }
                        )
                    } else {
                        DottedAddKey(
                            modifier = Modifier.weight(1f),
                            onClick = { onSlotEdit(slotIndex) }
                        )
                    }
                }
            }

            // Center-aligned text to touch island and open keyboard with minimal subtext
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
/**
 * Dotted '+' button for unassigned mapper slots. Click to add/assign key.
 */
@Composable
internal fun DottedAddKey(
    modifier: Modifier,
    onClick: () -> Unit,
    large: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val corner = if (large) 16.dp else 12.dp
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .height(if (large) 56.dp else 42.dp)
            .clip(shape)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawRoundRect(
                    color = outlineColor,
                    style = stroke,
                    cornerRadius = CornerRadius(corner.toPx())
                )
            }
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add key",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Minimal button displaying an assigned mapper key. Click to send, hold to replace.
 * Kept strictly minimal without subtext.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MapperKeyButton(
    modifier: Modifier,
    key: InteractiveKey,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    large: Boolean = false,
    containerColor: Color? = null
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(if (large) 56.dp else 42.dp)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let { hold ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        hold()
                    }
                }
            ),
        shape = RoundedCornerShape(if (large) 16.dp else 12.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val icon = key.icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = key.label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (large) 26.dp else 20.dp)
                )
            } else {
                Text(
                    // Mouse keys carry a compact label so this narrow row stays readable.
                    text = key.shortLabel ?: key.label,
                    style = if (large) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = if (large) 8.dp else 0.dp)
                )
            }
        }
    }
}

/**
 * Key picker bottom sheet to assign or replace a key for a mapper slot.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun KeyPickerSheet(
    slotIndex: Int,
    currentKey: InteractiveKey?,
    onSelectKey: (InteractiveKey) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Assign Key (Slot ${slotIndex + 1})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentKey != null) {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearKey()
                    }) {
                        Text("Reset to +", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                InteractiveKeyCategory.entries.forEach { category ->
                    item(key = category.name) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InteractiveKeys.keysIn(category).forEach { key ->
                                    val isSelected = key.token == currentKey?.token
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectKey(key)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = key.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

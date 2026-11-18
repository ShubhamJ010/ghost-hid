package com.sj010.ghid.ui.interactive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sj010.ghid.domain.model.AppSettings
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.common.shade.PullDownShade

/**
 * Notification-style key drawer for interactive mode.
 *
 * Closed: the same "keys" crest used by the gestures and payload shades.
 * Open: modifier toggles sit above every navigation, media and function key.
 */
@Composable
fun InteractiveKeyShade(
    targetOs: TargetOs,
    heldModifiers: Set<InteractiveModifier>,
    onKey: (InteractiveKey) -> Unit,
    mapperKeys: List<String>,
    onMapperSlotEdit: (Int) -> Unit,
    onToggleModifier: (InteractiveModifier) -> Unit,
    onOpenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dockHeight: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    PullDownShade(
        modifier = modifier,
        dockHeight = dockHeight,
        onOpenChanged = onOpenChanged,
        dock = { scope ->
            InteractiveMorphingDock(
                progress = scope.progress,
                onDismiss = { scope.dismiss() },
                onOpen = { scope.open() },
                mapperKeys = mapperKeys,
                onKey = onKey,
                onSlotEdit = onMapperSlotEdit,
                dragHandleModifier = scope.dragHandleModifier
            )
        },
        shade = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "modifiers") {
                        ModifierKeysSection(
                            targetOs = targetOs,
                            heldModifiers = heldModifiers,
                            onToggleModifier = onToggleModifier
                        )
                    }
                    InteractiveKeyCategory.entries.forEach { category ->
                        item(key = category.name) {
                            KeyCategorySection(
                                category = category,
                                onKey = onKey
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun ModifierKeysSection(
    targetOs: TargetOs,
    heldModifiers: Set<InteractiveModifier>,
    onToggleModifier: (InteractiveModifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Modifier keys",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            InteractiveKeys.modifiers.forEach { modifier ->
                val active = modifier in heldModifiers
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleModifier(modifier)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
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
                    Text(
                        text = modifier.labelFor(targetOs),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyCategorySection(
    category: InteractiveKeyCategory,
    onKey: (InteractiveKey) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${InteractiveKeys.keysIn(category).size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            InteractiveKeys.keysIn(category).forEach { key ->
                DrawerKeyChip(key = key, onClick = { onKey(key) })
            }
        }
    }
}

@Composable
private fun DrawerKeyChip(key: InteractiveKey, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 8.dp,
            bottomEnd = 14.dp,
            bottomStart = 8.dp
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Morphing dock crest. Closed it hosts the configurable mapper keys above the drag
 * pill; opening fades the keys away and reveals the close key, handing the body to
 * the full key catalog.
 */
@Composable
private fun InteractiveMorphingDock(
    progress: Float,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    mapperKeys: List<String>,
    onKey: (InteractiveKey) -> Unit,
    onSlotEdit: (Int) -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isClosed = progress < 0.15f
    val isOpen = progress > 0.85f
    val sideProgress = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
    val keysAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
    // Crest keys sit one step brighter than the island's mapper row.
    val crestColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        // Fills the dock slot so the closed crest matches the dashboard's device dock.
        modifier = dragHandleModifier
            .fillMaxSize()
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
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (keysAlpha > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = keysAlpha }
                        .padding(bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // First: add-your-own, which turns into whatever key it was given.
                        // It shares the island's free slot, so the two rows agree.
                        val customKey = InteractiveKeys.findByToken(mapperKeys.getOrElse(CUSTOM_SLOT) { "" })
                        if (customKey != null) {
                            MapperKeyButton(
                                modifier = Modifier.weight(1f),
                                key = customKey,
                                onClick = { onKey(customKey) },
                                onLongClick = { onSlotEdit(CUSTOM_SLOT) },
                                large = true,
                                containerColor = crestColor
                            )
                        } else {
                            DottedAddKey(
                                modifier = Modifier.weight(1f),
                                onClick = { onSlotEdit(CUSTOM_SLOT) },
                                large = true
                            )
                        }

                        // Then the two mouse clicks, in this order.
                        DOCK_MOUSE_KEYS.forEach { key ->
                            MapperKeyButton(
                                modifier = Modifier.weight(1f),
                                key = key,
                                onClick = { onKey(key) },
                                large = true,
                                containerColor = crestColor
                            )
                        }
                    }
                }
            }

            // Handle sits on the bottom edge of the crest, clear of the key row.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .graphicsLayer { alpha = keysAlpha }
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
            )

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDismiss()
                },
                enabled = isOpen,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .alpha(sideProgress)
                    .graphicsLayer {
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
                        contentDescription = "Close key drawer",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Left and right click, the two mouse buttons the crest always offers. */
private val DOCK_MOUSE_KEYS = listOf("lclick", "rclick").mapNotNull(InteractiveKeys::findByToken)

/** Mapper slot the crest's "add your own" button writes to (the island's free slot). */
private const val CUSTOM_SLOT = AppSettings.MAPPER_SLOTS - 1

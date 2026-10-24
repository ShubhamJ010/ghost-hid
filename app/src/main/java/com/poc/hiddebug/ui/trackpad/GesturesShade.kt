package com.poc.hiddebug.ui.trackpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.poc.hiddebug.service.HidService
import com.poc.hiddebug.ui.common.shade.PullDownShade
import com.poc.hiddebug.ui.theme.MaterialShapes
import kotlinx.coroutines.delay

/**
 * Gestures pull-down shade morphing directly out of the status bar on the Trackpad screen,
 * exactly matching the Snippets pull-down shade on the Combos screen.
 *
 * Closed: Seamlessly integrated into status bar (y=0, "gestures" title with drag handle pill below it).
 * Open: Morphs downward, drag handle animates to ABOVE "Gestures", OS toggle diamond and close button appear.
 */
@Composable
fun GesturesShade(
    service: HidService?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var targetOs by rememberSaveable { mutableStateOf(TargetOs.WINDOWS) }
    var selectedFilter by rememberSaveable { mutableStateOf<FingerCount?>(null) }
    var feedbackToast by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(feedbackToast) {
        if (feedbackToast != null) {
            delay(2200)
            feedbackToast = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullDownShade(
            modifier = modifier,
            dock = { shadeScope ->
                GestureMorphingDock(
                    progress = shadeScope.progress,
                    targetOs = targetOs,
                    onToggleOs = {
                        targetOs = if (targetOs == TargetOs.WINDOWS) TargetOs.MACOS else TargetOs.WINDOWS
                    },
                    onDismiss = { shadeScope.dismiss() },
                    onOpen = { shadeScope.open() },
                    dragHandleModifier = shadeScope.dragHandleModifier
                )
            },
            shade = { shadeScope ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    GestureShadeBody(
                        service = service,
                        targetOs = targetOs,
                        onToggleOs = {
                            targetOs = if (targetOs == TargetOs.WINDOWS) TargetOs.MACOS else TargetOs.WINDOWS
                        },
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        onTriggerGesture = { gesture ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val combo = gesture.comboFor(targetOs)
                            service?.executeCombo(combo)
                            feedbackToast = "⚡ Executed: ${gesture.fingers.label} ${gesture.title} (${gesture.actionName})"
                        }
                    )
                }
            },
            content = content
        )

        // Floating Toast Notification for executed gestures
        AnimatedVisibility(
            visible = feedbackToast != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            feedbackToast?.let { msg ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Gestures content body within the pulled-down shade.
 */
@Composable
fun GestureShadeBody(
    service: HidService?,
    targetOs: TargetOs,
    onToggleOs: () -> Unit,
    selectedFilter: FingerCount?,
    onFilterSelected: (FingerCount?) -> Unit,
    onTriggerGesture: (TrackpadGesture) -> Unit
) {
    val filteredGestures = remember(selectedFilter) {
        if (selectedFilter == null) {
            TrackpadGestureRegistry.ALL_GESTURES
        } else {
            TrackpadGestureRegistry.ALL_GESTURES.filter { it.fingers == selectedFilter }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 76.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header: OS Selector + Filter Chips
        item(span = { GridItemSpan(2) }) {
            GestureShadeControls(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                targetOs = targetOs,
                onToggleOs = onToggleOs
            )
        }

        // Gesture Cards (3 & 4 finger swipes)
        items(filteredGestures, key = { it.id }) { gesture ->
            GestureCard(
                gesture = gesture,
                onTrigger = { onTriggerGesture(gesture) }
            )
        }
    }
}

@Composable
private fun GestureShadeControls(
    selectedFilter: FingerCount?,
    onFilterSelected: (FingerCount?) -> Unit,
    targetOs: TargetOs,
    onToggleOs: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Trackpad Gestures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "3 & 4 finger precision shortcuts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OsToggleChip(targetOs = targetOs, onToggle = onToggleOs)
        }

        GestureFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )
    }
}

/**
 * Morphing dock element anchored at the bottom edge of the shade surface.
 *
 * In closed state (progress = 0):
 * - Text "gestures" is on top (y=4.dp), drag bar is directly BELOW the text (y=28.dp).
 * - Side buttons are invisible (scale = 0, alpha = 0).
 *
 * In open state (progress = 1):
 * - Left: Diamond OS button toggling Windows / macOS.
 * - Center: Drag bar animates to directly ABOVE the text "Gestures" (bar y=4.dp, text y=16.dp).
 * - Right: Circular close button.
 */
@Composable
fun GestureMorphingDock(
    progress: Float,
    targetOs: TargetOs,
    onToggleOs: () -> Unit,
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
            // LEFT: Diamond OS toggle button (appears as progress increases)
            if (progress > 0.08f) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleOs()
                    },
                    enabled = isOpen,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(46.dp)
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
                        Icon(
                            imageVector = if (targetOs == TargetOs.WINDOWS) Icons.Default.DesktopWindows else Icons.Default.Laptop,
                            contentDescription = "Toggle target OS",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // CENTER: Morphing Drag Bar and Title
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Drag pill bar: animates smoothly from BELOW the text to ABOVE the text
                Box(
                    modifier = Modifier
                        .offset(y = barOffsetY)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                )

                // Title Text: crossfades between "gestures" and "Gestures"
                Box(
                    modifier = Modifier.offset(y = textOffsetY),
                    contentAlignment = Alignment.Center
                ) {
                    if (closedTextAlpha > 0f) {
                        Text(
                            text = "gestures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.graphicsLayer { alpha = closedTextAlpha }
                        )
                    }
                    if (openTextAlpha > 0f) {
                        Text(
                            text = "Gestures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer { alpha = openTextAlpha }
                        )
                    }
                }
            }

            // RIGHT: Circular Close button (appears as progress increases)
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
                            contentDescription = "Close gestures shade",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

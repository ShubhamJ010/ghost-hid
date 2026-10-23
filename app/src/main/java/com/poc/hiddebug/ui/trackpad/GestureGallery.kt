package com.poc.hiddebug.ui.trackpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.hiddebug.ui.theme.MaterialShapes
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun ExpressiveGesturesGallery(
    onTriggerGesture: (TrackpadGesture) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<FingerCount?>(null) }
    var targetOs by remember { mutableStateOf(TargetOs.WINDOWS) }
    var lastPracticedGesture by remember { mutableStateOf<TrackpadGesture?>(null) }

    val filteredGestures = remember(selectedFilter) {
        if (selectedFilter == null) {
            TrackpadGestureRegistry.ALL_GESTURES
        } else {
            TrackpadGestureRegistry.ALL_GESTURES.filter { it.fingers == selectedFilter }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header & Controls
        item(span = { GridItemSpan(2) }) {
            GestureGalleryControlBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                targetOs = targetOs,
                onOsToggled = {
                    targetOs = if (targetOs == TargetOs.WINDOWS) TargetOs.MACOS else TargetOs.WINDOWS
                }
            )
        }

        // Gesture Cards
        items(filteredGestures, key = { it.id }) { gesture ->
            GestureCard(
                gesture = gesture,
                targetOs = targetOs,
                onTrigger = { onTriggerGesture(gesture) }
            )
        }

        // Interactive Practice Pad
        item(span = { GridItemSpan(2) }) {
            GesturePracticePad(
                targetOs = targetOs,
                onGestureRecognized = { gesture ->
                    lastPracticedGesture = gesture
                    onTriggerGesture(gesture)
                }
            )
        }
    }
}

@Composable
fun GestureGalleryControlBar(
    selectedFilter: FingerCount?,
    onFilterSelected: (FingerCount?) -> Unit,
    targetOs: TargetOs,
    onOsToggled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialShapes.ShapeFlower)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Touch Gestures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "3 & 4 finger precision swipes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // OS Platform Toggle Chip
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOsToggled)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (targetOs == TargetOs.WINDOWS) Icons.Default.DesktopWindows else Icons.Default.Laptop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = targetOs.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Filter chips: All, 3 Fingers, 4 Fingers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All (8)") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = selectedFilter == FingerCount.THREE,
                onClick = { onFilterSelected(FingerCount.THREE) },
                label = { Text("3 Fingers (4)") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = selectedFilter == FingerCount.FOUR,
                onClick = { onFilterSelected(FingerCount.FOUR) },
                label = { Text("4 Fingers (4)") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun GestureCard(
    gesture: TrackpadGesture,
    targetOs: TargetOs,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    var isExecuting by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale.value),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Expressive Shape Badge + Finger Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(gesture.resolveShape())
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = gesture.resolveIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (gesture.fingers == FingerCount.THREE) "3👆" else "4👆",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Gesture Titles
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = gesture.actionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gesture.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Keyboard Shortcut Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Text(
                    text = gesture.shortcutFor(targetOs),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Trigger Button
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isExecuting = true
                    scope.launch {
                        pressScale.animateTo(0.94f, tween(80))
                        pressScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
                        isExecuting = false
                    }
                    onTrigger()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Execute",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Interactive Practice Pad in the gallery allowing live multi-finger swipe testing.
 */
@Composable
fun GesturePracticePad(
    targetOs: TargetOs,
    onGestureRecognized: (TrackpadGesture) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 45.dp.toPx() } }

    var activeFingerCount by remember { mutableIntStateOf(0) }
    var recognizedFeedback by remember { mutableStateOf<String?>(null) }
    var recognizedIcon by remember { mutableStateOf<TrackpadGesture?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(targetOs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var gestureFired = false
                    val startPositions = mutableMapOf(down.id to down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedList = event.changes.filter { it.pressed }
                        activeFingerCount = pressedList.size

                        if (pressedList.isEmpty()) {
                            activeFingerCount = 0
                            break
                        }

                        // Collect new pointers
                        pressedList.forEach { change ->
                            if (!startPositions.containsKey(change.id)) {
                                startPositions[change.id] = change.position
                            }
                        }

                        // Recognize 3 or 4 finger swipe
                        if (!gestureFired && pressedList.size in 3..4 && startPositions.size in 3..4) {
                            var dxSum = 0f
                            var dySum = 0f
                            var validPointers = 0

                            pressedList.forEach { change ->
                                val start = startPositions[change.id]
                                if (start != null) {
                                    dxSum += (change.position.x - start.x)
                                    dySum += (change.position.y - start.y)
                                    validPointers++
                                }
                            }

                            if (validPointers >= 3) {
                                val avgDx = dxSum / validPointers
                                val avgDy = dySum / validPointers
                                val distance = kotlin.math.hypot(avgDx, avgDy)

                                if (distance >= swipeThresholdPx) {
                                    val fingers = if (validPointers >= 4) FingerCount.FOUR else FingerCount.THREE
                                    val direction = if (abs(avgDy) > abs(avgDx)) {
                                        if (avgDy < 0) GestureDirection.UP else GestureDirection.DOWN
                                    } else {
                                        if (avgDx < 0) GestureDirection.LEFT else GestureDirection.RIGHT
                                    }

                                    val match = TrackpadGestureRegistry.find(fingers, direction)
                                    if (match != null) {
                                        gestureFired = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        recognizedIcon = match
                                        recognizedFeedback = "${match.fingers.label} ${match.title} → ${match.actionName}"
                                        onGestureRecognized(match)
                                        pressedList.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = recognizedIcon != null,
                    enter = scaleIn(spring(stiffness = 500f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    recognizedIcon?.let { gesture ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = gesture.resolveIcon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${gesture.fingers.label} ${gesture.title}: ${gesture.shortcutFor(targetOs)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (recognizedIcon == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialShapes.ShapeArch)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (activeFingerCount > 0) "$activeFingerCount finger(s) on pad" else "Gesture Practice Pad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Swipe here with 3 or 4 fingers to test live OS responses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

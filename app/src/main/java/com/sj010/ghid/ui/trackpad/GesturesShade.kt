package com.sj010.ghid.ui.trackpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.common.shade.PullDownShade

/**
 * Gestures pull-down shade morphing directly out of the status bar on the Trackpad screen.
 *
 * Closed: Seamlessly integrated into status bar (y=0, "gestures" title with drag handle pill below it).
 * Open: Morphs downward, drag handle animates to ABOVE "Gestures", OS toggle button and close button appear.
 */
@Composable
fun GesturesShade(
    service: HidService?,
    modifier: Modifier = Modifier,
    targetOs: TargetOs = service?.effectiveTargetOs?.collectAsState()?.value ?: TargetOs.WINDOWS,
    onToggleOs: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf<FingerCount?>(null) }
    val haptic = LocalHapticFeedback.current

    PullDownShade(
        modifier = modifier,
        dockHeight = 56.dp,
        openDockHeight = 56.dp,
        dock = { shadeScope ->
            GestureMorphingDock(
                progress = shadeScope.progress,
                targetOs = targetOs,
                onToggleOs = onToggleOs,
                onDismiss = { shadeScope.dismiss() },
                onOpen = { shadeScope.open() },
                dragHandleModifier = shadeScope.dragHandleModifier
            )
        },
        shade = { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                GestureGallery(
                    targetOs = targetOs,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    onTriggerGesture = { gesture ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        service?.executeCombo(gesture.comboFor(targetOs))
                    }
                )
            }
        },
        content = content
    )
}

/**
 * Morphing Dock bar positioned at the bottom edge of the shade surface.
 *
 * In closed state (progress = 0):
 * - Text "gestures" is on top (y=4.dp), drag bar is directly BELOW the text (y=28.dp).
 * - Side buttons are invisible (scale = 0, alpha = 0).
 *
 * In open state (progress = 1):
 * - Left: Circular OS button toggling Windows / macOS / Linux.
 * - Center: Drag bar animates to directly ABOVE the text "Gestures" (bar y=4.dp, text y=16.dp).
 * - Right: Circular close button.
 */
@Composable
private fun GestureMorphingDock(
    progress: Float,
    targetOs: TargetOs,
    onToggleOs: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    dragHandleModifier: Modifier = Modifier
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
        modifier = dragHandleModifier
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
            // LEFT: OS toggle button (appears as progress increases)
            if (progress > 0.08f) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleOs()
                    },
                    enabled = isOpen,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .graphicsLayer {
                            alpha = sideProgress
                            scaleX = 0.3f + 0.7f * sideProgress
                            scaleY = 0.3f + 0.7f * sideProgress
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    tonalElevation = 3.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OsGlyph(
                            os = targetOs,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
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

package com.sj010.ghid.ui.snippets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.common.shade.PullDownShade
import com.sj010.ghid.ui.theme.MaterialShapes

/**
 * Snippets shade morphing directly out of the status bar.
 *
 * Closed: Seamlessly integrated into the status bar area (starts at y=0,
 * status bar icons sit in top, "snippets" title with drag handle pill below it).
 *
 * Open: The surface morphs downward, the drag bar animates to ABOVE the text "Snippet",
 * and the diamond filter button + circle close button animate into view!
 */
@Composable
fun SnippetsShade(
    service: HidService?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var filterQuery by rememberSaveable { mutableStateOf("") }

    PullDownShade(
        modifier = modifier,
        dock = { shadeScope ->
            SnippetMorphingDock(
                progress = shadeScope.progress,
                showFilter = showFilter,
                hasFilterQuery = filterQuery.isNotBlank(),
                onToggleFilter = { showFilter = !showFilter },
                onDismiss = { shadeScope.dismiss() },
                onOpen = { shadeScope.open() },
                dragHandleModifier = shadeScope.dragHandleModifier
            )
        },
        shade = { shadeScope ->
            SnippetShadeContent(
                service = service,
                showFilter = showFilter,
                filterQuery = filterQuery,
                onQueryChange = { filterQuery = it },
                onCloseFilter = { showFilter = false }
            )
        },
        content = content
    )
}

/**
 * Morphing dock element anchored at the bottom edge of the shade surface.
 *
 * In closed state (progress = 0):
 * - Text "snippets" is on top (y=4.dp), drag bar is directly BELOW the text (y=28.dp).
 * - Side buttons are invisible (scale = 0, alpha = 0).
 *
 * In open state (progress = 1):
 * - Left: Diamond filter toggle button.
 * - Center: Drag bar animates to directly ABOVE the text "Snippet" (bar y=4.dp, text y=16.dp).
 * - Right: Circular close button.
 */
@Composable
fun SnippetMorphingDock(
    progress: Float,
    showFilter: Boolean,
    hasFilterQuery: Boolean,
    onToggleFilter: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isClosed = progress < 0.15f
    val isOpen = progress > 0.85f

    // Side buttons (diamond filter on left, circle close on right)
    // Blossom into view as progress goes from 0.15 to 1.0
    val sideProgress = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)

    // Center pill bar and text positions
    // In closed state (progress = 0): text is at y = 4.dp, bar is at y = 28.dp (BELOW text)
    // In open state (progress = 1): bar is at y = 4.dp, text is at y = 16.dp (ABOVE text)
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
            // LEFT: Diamond filter button (appears as progress increases)
            if (progress > 0.08f) {
                val filterActive = showFilter || hasFilterQuery
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFilter()
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
                    color = if (filterActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (filterActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (filterActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (filterActive) 3.dp else 1.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = if (showFilter) "Hide filter" else "Show filter",
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
                // Drag pill bar: animates smoothly from BELOW the text to ABOVE the text!
                Box(
                    modifier = Modifier
                        .offset(y = barOffsetY)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                )

                // Title Text: crossfades between "snippets" and "Snippet"
                Box(
                    modifier = Modifier.offset(y = textOffsetY),
                    contentAlignment = Alignment.Center
                ) {
                    if (closedTextAlpha > 0f) {
                        Text(
                            text = "snippets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.graphicsLayer { alpha = closedTextAlpha }
                        )
                    }
                    if (openTextAlpha > 0f) {
                        Text(
                            text = "Snippet",
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
                            contentDescription = "Close snippets shade",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

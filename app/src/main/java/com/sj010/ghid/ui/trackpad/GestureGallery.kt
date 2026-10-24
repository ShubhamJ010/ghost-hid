package com.sj010.ghid.ui.trackpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sj010.ghid.domain.model.TargetOs

/**
 * Gesture gallery: a two-column grid of 3- and 4-finger trackpad swipes.
 *
 * Design contract — keep it minimal:
 * - A card's headline is the swipe the user performs ("Swipe Up"), never the resulting action.
 * - The action appears once, as a short label ("Task View").
 * - Keyboard shortcuts are an implementation detail and are never rendered.
 *
 * State (selected finger filter) is hoisted to the caller so the same gallery can be
 * embedded in the gesture shade or anywhere else.
 *
 * @param targetOs host OS used to resolve each action label and the combo that will be sent.
 * @param selectedFilter finger-count filter; `null` shows every gesture.
 * @param onFilterSelected invoked with the newly picked filter (`null` = all).
 * @param onTriggerGesture called when a card is tapped.
 * @param contentPadding extra scroll padding, e.g. room for a floating dock.
 */
@Composable
fun GestureGallery(
    targetOs: TargetOs,
    selectedFilter: FingerCount?,
    onFilterSelected: (FingerCount?) -> Unit,
    onTriggerGesture: (TrackpadGesture) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(top = 12.dp, bottom = 84.dp)
) {
    val gestures = remember(selectedFilter) {
        TrackpadGestureRegistry.ALL_GESTURES
            .filter { selectedFilter == null || it.fingers == selectedFilter }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            GestureFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected
            )
        }

        items(gestures, key = { it.id }) { gesture ->
            GestureCard(
                gesture = gesture,
                targetOs = targetOs,
                onTrigger = { onTriggerGesture(gesture) }
            )
        }
    }
}

/**
 * Pill row that narrows the grid to three or four fingers. No counts or prose —
 * the grid itself shows how many gestures exist.
 */
@Composable
internal fun GestureFilterChips(
    selectedFilter: FingerCount?,
    onFilterSelected: (FingerCount?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GestureFilterChip(
            selected = selectedFilter == null,
            label = "All",
            onClick = { onFilterSelected(null) }
        )
        FingerCount.entries.forEach { fingers ->
            GestureFilterChip(
                selected = selectedFilter == fingers,
                label = fingers.label,
                onClick = { onFilterSelected(fingers) }
            )
        }
    }
}

/** Single M3 Expressive chip; circular for the softer, playful gallery look. */
@Composable
private fun GestureFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * One gesture tile. Tapping anywhere on the tile performs the gesture on the host.
 *
 * Layout is deliberately two lines: [TrackpadGesture.title] (the swipe) and the
 * host action label. The expressive badge carries the direction arrow, and
 * [FingerDots] carries the finger count, so no explanatory sentence is needed.
 * The badge is inverted against the card: light shape (`primary`) with a dark
 * arrow (`onPrimary`), the same light-on-dark pairing as the dashboard's Connect pill.
 */
@Composable
fun GestureCard(
    gesture: TrackpadGesture,
    targetOs: TargetOs = TargetOs.WINDOWS,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onTrigger,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(gesture.resolveShape())
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = gesture.resolveIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                FingerDots(count = gesture.fingers.count)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = gesture.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gesture.actionNameFor(targetOs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Text-free finger count: one dot per finger, quieter than a "3 Fingers" pill.
 */
@Composable
private fun FingerDots(count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            )
        }
    }
}

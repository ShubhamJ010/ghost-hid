package com.sj010.ghid.ui.streamdeck

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.ui.common.os.ComboGlyph
import com.sj010.ghid.ui.theme.MaterialShapes

/**
 * One full-bleed deck slot. Positions are fractions of the available landscape
 * canvas so the same five pins spread out on every phone size without a grid.
 */
data class StreamDeckSlot(
    val slotId: Int,
    val shapeName: String,
    val emptyLabel: String,
    val shape: Shape,
    val width: Dp,
    val height: Dp,
    val centerX: Float,
    val centerY: Float,
    val rotation: Float,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

private data class StreamDeckPalette(
    val container: Color,
    val content: Color,
    val border: Color
)

/**
 * Landscape stream-deck variant of the dashboard collage. It keeps the exact
 * same five pin slots, but swaps in distinct M3 Expressive silhouettes and
 * scatters them across the whole display instead of stacking them in a column.
 */
fun streamDeckSlots(): List<StreamDeckSlot> {
    val violet = StreamDeckPalette(
        container = Color(0xFF4B3280),
        content = Color(0xFFF3EDF7),
        border = Color(0xFFD0BCFF)
    )
    val jade = StreamDeckPalette(
        container = Color(0xFF00513B),
        content = Color(0xFFA7F8D6),
        border = Color(0xFF86F8CE)
    )
    val amber = StreamDeckPalette(
        container = Color(0xFF943C00),
        content = Color(0xFFFFDCC4),
        border = Color(0xFFFFB68F)
    )
    val ocean = StreamDeckPalette(
        container = Color(0xFF004C6E),
        content = Color(0xFFCCE6FF),
        border = Color(0xFF92CDFF)
    )
    val rose = StreamDeckPalette(
        container = Color(0xFF8C1D40),
        content = Color(0xFFFFD9E2),
        border = Color(0xFFFFB1C8)
    )

    return listOf(
        // Centre anchor: expressive pill, tilted just enough to feel hand-set.
        StreamDeckSlot(
            slotId = 0,
            shapeName = "Pill",
            emptyLabel = "Pill",
            shape = MaterialShapes.ShapePill,
            width = 156.dp,
            height = 104.dp,
            centerX = 0.50f,
            centerY = 0.53f,
            rotation = 18f,
            containerColor = violet.container,
            contentColor = violet.content,
            borderColor = violet.border
        ),
        // Upper-left clover.
        StreamDeckSlot(
            slotId = 1,
            shapeName = "Clover",
            emptyLabel = "Clover",
            shape = MaterialShapes.ShapeClover4,
            width = 120.dp,
            height = 120.dp,
            centerX = 0.155f,
            centerY = 0.255f,
            rotation = -10f,
            containerColor = jade.container,
            contentColor = jade.content,
            borderColor = jade.border
        ),
        // Upper-right heart.
        StreamDeckSlot(
            slotId = 2,
            shapeName = "Heart",
            emptyLabel = "Heart",
            shape = MaterialShapes.ShapeHeart,
            width = 128.dp,
            height = 116.dp,
            centerX = 0.845f,
            centerY = 0.255f,
            rotation = 8f,
            containerColor = amber.container,
            contentColor = amber.content,
            borderColor = amber.border
        ),
        // Lower-left ghost.
        StreamDeckSlot(
            slotId = 3,
            shapeName = "Ghost",
            emptyLabel = "Ghost",
            shape = MaterialShapes.ShapeGhostish,
            width = 118.dp,
            height = 110.dp,
            centerX = 0.16f,
            centerY = 0.775f,
            rotation = 6f,
            containerColor = ocean.container,
            contentColor = ocean.content,
            borderColor = ocean.border
        ),
        // Lower-right sunny.
        StreamDeckSlot(
            slotId = 4,
            shapeName = "Sunny",
            emptyLabel = "Sunny",
            shape = MaterialShapes.ShapeSunny,
            width = 124.dp,
            height = 112.dp,
            centerX = 0.84f,
            centerY = 0.775f,
            rotation = -6f,
            containerColor = rose.container,
            contentColor = rose.content,
            borderColor = rose.border
        )
    )
}

@Composable
fun StreamDeckCollage(
    pinnedBySlot: Map<Int, Combo>,
    slots: List<StreamDeckSlot>,
    onRunCombo: (Combo) -> Unit,
    onOpenPicker: (StreamDeckSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        // Scale the silhouettes with the short edge so a 5.5" and a 6.9" phone
        // both keep the same hand-spread composition.
        val scale = (minOf(maxWidth, maxHeight) / 330.dp).coerceIn(0.78f, 1.16f)

        slots.forEach { slot ->
            val width = slot.width * scale
            val height = slot.height * scale
            val combo = pinnedBySlot[slot.slotId]

            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * slot.centerX - width / 2,
                        y = maxHeight * slot.centerY - height / 2
                    )
                    .size(width, height)
            ) {
                if (combo != null) {
                    DeckShapeButton(
                        combo = combo,
                        slot = slot,
                        width = width,
                        height = height,
                        onClick = { onRunCombo(combo) },
                        onLongClick = { onOpenPicker(slot) }
                    )
                } else {
                    DeckEmptyShape(
                        slot = slot,
                        width = width,
                        height = height,
                        onClick = { onOpenPicker(slot) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.DeckShapeButton(
    combo: Combo,
    slot: StreamDeckSlot,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "deckShapeScale"
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .scale(animatedScale)
            .rotate(slot.rotation)
            .clip(slot.shape)
            .background(slot.containerColor)
            .border(BorderStroke(1.5.dp, slot.borderColor.copy(alpha = 0.45f)), slot.shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .rotate(-slot.rotation)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            ComboGlyph(
                combo = combo,
                size = 32.dp,
                tint = slot.contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = combo.name,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = slot.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.DeckEmptyShape(
    slot: StreamDeckSlot,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "deckEmptyScale"
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .scale(animatedScale)
            .rotate(slot.rotation)
            .clip(slot.shape)
            .background(slot.containerColor.copy(alpha = 0.22f))
            .border(
                BorderStroke(1.5.dp, slot.contentColor.copy(alpha = 0.55f)),
                slot.shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .rotate(-slot.rotation)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Pin a favorite combo to the ${slot.emptyLabel} shape",
                modifier = Modifier.size(28.dp),
                tint = slot.contentColor.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = slot.emptyLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = slot.contentColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

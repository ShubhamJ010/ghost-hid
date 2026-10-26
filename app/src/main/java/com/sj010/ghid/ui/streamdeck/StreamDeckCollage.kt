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
import com.sj010.ghid.ui.theme.ExpressiveShapes

/**
 * One deck tile. Sizes are fractions of the deck canvas so the same composition
 * covers the whole display on every landscape aspect ratio.
 */
data class StreamDeckSlot(
    val slotId: Int,
    val shapeName: String,
    val emptyLabel: String,
    val shape: Shape,
    /** Shape height as a fraction of the deck height. */
    val heightFraction: Float,
    /** Width divided by height, so wide silhouettes stay wide. */
    val aspect: Float,
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

private val deckPalettes = listOf(
    StreamDeckPalette(Color(0xFF4B3280), Color(0xFFF3EDF7), Color(0xFFD0BCFF)),
    StreamDeckPalette(Color(0xFF00513B), Color(0xFFA7F8D6), Color(0xFF86F8CE)),
    StreamDeckPalette(Color(0xFF943C00), Color(0xFFFFDCC4), Color(0xFFFFB68F)),
    StreamDeckPalette(Color(0xFF004C6E), Color(0xFFCCE6FF), Color(0xFF92CDFF)),
    StreamDeckPalette(Color(0xFF8C1D40), Color(0xFFFFD9E2), Color(0xFFFFB1C8)),
    StreamDeckPalette(Color(0xFF3F2E00), Color(0xFFFFE9A8), Color(0xFFF2D277))
)

/** One deck row entry: shape, name, x centre, y centre, height fraction, tilt. */
private data class DeckEntry(
    val shapeName: String,
    val shape: Shape,
    val centerX: Float,
    val centerY: Float,
    val heightFraction: Float,
    val rotation: Float
)

private val deckEntries = listOf(
    // Upper band: the five dashboard pins land on slots 0..4.
    DeckEntry("Pill", ExpressiveShapes.Pill, 0.12f, 0.30f, 0.40f, 6f),
    DeckEntry("Clover", ExpressiveShapes.Clover4Leaf, 0.31f, 0.30f, 0.40f, -6f),
    DeckEntry("Heart", ExpressiveShapes.Heart, 0.50f, 0.30f, 0.40f, 4f),
    DeckEntry("Ghost", ExpressiveShapes.Ghostish, 0.69f, 0.30f, 0.40f, -5f),
    DeckEntry("Sunny", ExpressiveShapes.Sunny, 0.88f, 0.30f, 0.40f, 6f),
    // Lower band: the rest of the deck, empty until a combo is pinned there.
    DeckEntry("Cookie", ExpressiveShapes.Cookie4Sided, 0.10f, 0.76f, 0.34f, -4f),
    DeckEntry("Flower", ExpressiveShapes.Flower, 0.26f, 0.76f, 0.34f, 5f),
    DeckEntry("Arch", ExpressiveShapes.Arch, 0.42f, 0.76f, 0.34f, -3f),
    DeckEntry("Gem", ExpressiveShapes.Gem, 0.58f, 0.76f, 0.34f, 4f),
    DeckEntry("Fan", ExpressiveShapes.Fan, 0.74f, 0.76f, 0.34f, -5f),
    DeckEntry("Bun", ExpressiveShapes.Bun, 0.90f, 0.76f, 0.34f, 3f)
)

/**
 * Full-bleed landscape stream deck. It is its own composition rather than the
 * dashboard collage: eleven expressive tiles fill the display, and the five
 * dashboard pin slots (0..4) map onto the upper band so the same favourites
 * light up here. The remaining tiles are free slots for this view.
 */
fun streamDeckSlots(): List<StreamDeckSlot> = deckEntries.mapIndexed { index, entry ->
    val palette = deckPalettes[index % deckPalettes.size]
    StreamDeckSlot(
        slotId = index,
        shapeName = entry.shapeName,
        emptyLabel = entry.shapeName,
        shape = entry.shape,
        heightFraction = entry.heightFraction,
        aspect = 1f,
        centerX = entry.centerX,
        centerY = entry.centerY,
        rotation = entry.rotation,
        containerColor = palette.container,
        contentColor = palette.content,
        borderColor = palette.border
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
        slots.forEach { slot ->
            val height = maxHeight * slot.heightFraction
            val width = (height * slot.aspect).coerceAtMost(maxWidth * 0.4f)
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
                .padding(horizontal = width * 0.14f, vertical = height * 0.10f)
        ) {
            ComboGlyph(
                combo = combo,
                size = (height.value * 0.26f).coerceIn(26f, 44f).dp,
                tint = slot.contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = combo.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = (height.value * 0.095f).coerceIn(11f, 16f).sp
                ),
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
                .padding(horizontal = width * 0.12f, vertical = height * 0.08f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Pin a favorite combo to the ${slot.emptyLabel} shape",
                modifier = Modifier.size((height.value * 0.24f).coerceIn(22f, 38f).dp),
                tint = slot.contentColor.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = slot.emptyLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (height.value * 0.08f).coerceIn(10f, 14f).sp
                ),
                fontWeight = FontWeight.Bold,
                color = slot.contentColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

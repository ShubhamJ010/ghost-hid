package com.sj010.ghid.ui.dashboard

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.PinnedFavoritesPolicy
import com.sj010.ghid.ui.theme.MaterialShapes

/**
 * Dashboard "fav pinned collage": five expressive shapes, each an empty add
 * slot until the user pins a starred combo to it.
 *
 * Tap a pinned shape to run its combo. Long-press it to pick a different
 * favorite. Empty shapes open the same picker. A favorite can only live on one
 * shape at a time.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavPinnedCollage(
    combos: List<Combo>,
    pinnedBySlot: Map<Int, Combo>,
    onRunCombo: (Combo) -> Unit,
    onPinCombo: (slotId: Int, comboName: String) -> Unit,
    onBrowseCombos: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val favorites = remember(combos) { PinnedFavoritesPolicy.favorites(combos) }
    val configs = remember { collageConfigs() }
    val shapeNames = remember(configs) { configs.associate { it.slotId to it.shapeName } }
    var pickerSlot by remember { mutableStateOf<CollageConfig?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            configs.forEach { cfg ->
                val combo = pinnedBySlot[cfg.slotId]
                if (combo != null) {
                    CollageShapeButton(
                        combo = combo,
                        config = cfg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRunCombo(combo)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pickerSlot = cfg
                        }
                    )
                } else {
                    CollageEmptyShapeSlot(
                        config = cfg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pickerSlot = cfg
                        }
                    )
                }
            }
        }
    }

    pickerSlot?.let { config ->
        FavoritePickerSheet(
            config = config,
            favorites = favorites,
            pinnedBySlot = pinnedBySlot,
            shapeNames = shapeNames,
            onSelect = { combo ->
                onPinCombo(config.slotId, combo.name)
                pickerSlot = null
            },
            onBrowseCombos = {
                pickerSlot = null
                onBrowseCombos()
            },
            onDismiss = { pickerSlot = null }
        )
    }
}

private data class ExpressiveFlatPalette(
    val container: Color,
    val content: Color,
    val border: Color
)

data class CollageConfig(
    val slotId: Int,
    val shapeName: String,
    val emptyLabel: String,
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val align: Alignment,
    val rot: Float,
    val shape: Shape,
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp,
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke
)

private fun collageConfigs(): List<CollageConfig> {
    // M3 Expressive flat palettes, one per shape. Order matters: it drives the slots.
    val palettes = listOf(
        // 0: Deep Royal Violet / Lilac (Center Pill - Login)
        ExpressiveFlatPalette(
            container = Color(0xFF4B3280),
            content = Color(0xFFF3EDF7),
            border = Color(0xFFD0BCFF)
        ),
        // 1: Expressive Jade / Mint (TopStart Squircle - Lock)
        ExpressiveFlatPalette(
            container = Color(0xFF00513B),
            content = Color(0xFFA7F8D6),
            border = Color(0xFF86F8CE)
        ),
        // 2: Expressive Sunset Amber / Peach (BottomEnd 6-Point Star - Sleep)
        ExpressiveFlatPalette(
            container = Color(0xFF943C00),
            content = Color(0xFFFFDCC4),
            border = Color(0xFFFFB68F)
        ),
        // 3: Expressive Ocean Blue (TopEnd Fan - Fan)
        ExpressiveFlatPalette(
            container = Color(0xFF004C6E),
            content = Color(0xFFCCE6FF),
            border = Color(0xFF92CDFF)
        ),
        // 4: Expressive Raspberry / Rose (BottomStart Ghost-ish - Ghost-ish)
        ExpressiveFlatPalette(
            container = Color(0xFF8C1D40),
            content = Color(0xFFFFD9E2),
            border = Color(0xFFFFB1C8)
        )
    )

    val p0 = palettes[0]
    val p1 = palettes[1]
    val p2 = palettes[2]
    val p3 = palettes[3]
    val p4 = palettes[4]

    return listOf(
        // 0: Center Tilted Pill (Login)
        CollageConfig(
            slotId = 0,
            shapeName = "Pill",
            emptyLabel = "Pill",
            width = 110.dp,
            height = 172.dp,
            align = Alignment.Center,
            rot = 42f,
            shape = RoundedCornerShape(percent = 50),
            offsetX = (-6).dp,
            offsetY = 14.dp,
            containerColor = p0.container,
            contentColor = p0.content,
            border = BorderStroke(1.5.dp, p0.border.copy(alpha = 0.40f))
        ),
        // 1: TopStart Squircle (Lock - enlarged to 126dp and shifted down)
        CollageConfig(
            slotId = 1,
            shapeName = "Squircle",
            emptyLabel = "Squircle",
            width = 126.dp,
            height = 126.dp,
            align = Alignment.TopStart,
            rot = -12f,
            shape = RoundedCornerShape(30.dp),
            offsetX = 12.dp,
            offsetY = 38.dp,
            containerColor = p1.container,
            contentColor = p1.content,
            border = BorderStroke(1.5.dp, p1.border.copy(alpha = 0.40f))
        ),
        // 2: BottomEnd 6-Sided Rounded Star (Sleep - enlarged 10% to 150dp, snug towards center)
        CollageConfig(
            slotId = 2,
            shapeName = "Flower",
            emptyLabel = "Flower",
            width = 150.dp,
            height = 150.dp,
            align = Alignment.BottomEnd,
            rot = 8f,
            shape = com.sj010.ghid.ui.theme.RoundedStarShape(sides = 6, curve = 0.09, rotation = 45f),
            offsetX = (-16).dp,
            offsetY = (-10).dp,
            containerColor = p2.container,
            contentColor = p2.content,
            border = BorderStroke(1.5.dp, p2.border.copy(alpha = 0.40f))
        ),
        // 3: TopEnd Fan (M3 Expressive ShapeFan)
        CollageConfig(
            slotId = 3,
            shapeName = "Fan",
            emptyLabel = "Fan",
            width = 112.dp,
            height = 112.dp,
            align = Alignment.TopEnd,
            rot = -4f,
            shape = MaterialShapes.ShapeFan,
            offsetX = (-14).dp,
            offsetY = 10.dp,
            containerColor = p3.container,
            contentColor = p3.content,
            border = BorderStroke(1.5.dp, p3.border.copy(alpha = 0.40f))
        ),
        // 4: BottomStart Ghost-ish (M3 Expressive ShapeGhostish - shifted down & scattered)
        CollageConfig(
            slotId = 4,
            shapeName = "Ghost-ish",
            emptyLabel = "Ghost-ish",
            width = 114.dp,
            height = 114.dp,
            align = Alignment.BottomStart,
            rot = 6f,
            shape = MaterialShapes.ShapeGhostish,
            offsetX = 18.dp,
            offsetY = 4.dp,
            containerColor = p4.container,
            contentColor = p4.content,
            border = BorderStroke(1.5.dp, p4.border.copy(alpha = 0.40f))
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.CollageShapeButton(
    combo: Combo,
    config: CollageConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "shapeScale"
    )

    Box(
        modifier = Modifier
            .size(config.width, config.height)
            .align(config.align)
            .offset(config.offsetX, config.offsetY)
            .scale(animatedScale)
            .rotate(config.rot)
            .clip(config.shape)
            .background(config.containerColor)
            .border(config.border, config.shape)
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
                .rotate(-config.rot)
                .padding(6.dp)
        ) {
            val emojiSize = when {
                config.width >= 125.dp || config.height >= 160.dp -> 32.sp
                config.width >= 105.dp -> 28.sp
                else -> 24.sp
            }
            val titleStyle = when {
                config.width >= 125.dp || config.height >= 160.dp -> MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                config.width >= 105.dp -> MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp)
                else -> MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
            }

            Text(
                text = combo.emoji?.ifEmpty { "⚡" } ?: "⚡",
                fontSize = emojiSize
            )
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = combo.name,
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = config.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.CollageEmptyShapeSlot(
    config: CollageConfig,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "emptyShapeScale"
    )

    Box(
        modifier = Modifier
            .size(config.width, config.height)
            .align(config.align)
            .offset(config.offsetX, config.offsetY)
            .scale(animatedScale)
            .rotate(config.rot)
            .clip(config.shape)
            .background(config.containerColor.copy(alpha = 0.22f))
            .border(
                BorderStroke(1.5.dp, config.contentColor.copy(alpha = 0.55f)),
                config.shape
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
                .rotate(-config.rot)
                .padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Pin a favorite combo to the ${config.emptyLabel} shape",
                modifier = Modifier.size(26.dp),
                tint = config.contentColor.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = config.emptyLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = config.contentColor.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritePickerSheet(
    config: CollageConfig,
    favorites: List<Combo>,
    pinnedBySlot: Map<Int, Combo>,
    shapeNames: Map<Int, String>,
    onSelect: (Combo) -> Unit,
    onBrowseCombos: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(config.shape)
                        .background(config.containerColor.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = config.contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pin a favorite",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose a combo for the ${config.emptyLabel} shape",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (favorites.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No favorite combos yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Star a combo in Combos, then come back and pin it to this shape.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onBrowseCombos) {
                        Text("Browse combos")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(top = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites, key = { PinnedFavoritesPolicy.normalize(it.name) }) { combo ->
                        val pinnedSlot = pinnedBySlot.entries
                            .firstOrNull {
                                PinnedFavoritesPolicy.normalize(it.value.name) ==
                                    PinnedFavoritesPolicy.normalize(combo.name)
                            }
                            ?.key
                        FavoritePickerRow(
                            combo = combo,
                            pinnedShape = pinnedSlot
                                ?.takeIf { it != config.slotId }
                                ?.let { shapeNames[it] },
                            isCurrentShape = pinnedSlot == config.slotId,
                            onClick = { onSelect(combo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritePickerRow(
    combo: Combo,
    pinnedShape: String?,
    isCurrentShape: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = combo.emoji?.ifEmpty { "⚡" } ?: "⚡", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = combo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = combo.keys,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            when {
                isCurrentShape -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Pinned to this shape",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                pinnedShape != null -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = "On $pinnedShape",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Pin ${combo.name}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

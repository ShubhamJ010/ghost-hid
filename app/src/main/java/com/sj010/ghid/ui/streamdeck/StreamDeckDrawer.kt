package com.sj010.ghid.ui.streamdeck

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Landscape-only side drawer. A slim handle lives on the middle of the left
 * edge; tapping or dragging it rightwards slides the payload panel in from the
 * left, and dragging the panel back left (or tapping the scrim) closes it.
 */
@Composable
fun StreamDeckDrawer(
    open: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    panel: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "deckDrawer"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelWidth = (maxWidth * 0.46f).coerceAtMost(460.dp)
        val panelWidthPx = with(LocalDensity.current) { panelWidth.toPx() }

        // Dim the deck while the panel is out.
        if (progress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = -panelWidthPx * (1f - progress) }
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged < -60f) onClose()
                        },
                        onDragCancel = { dragged = 0f },
                        onHorizontalDrag = { _, amount -> dragged += amount }
                    )
                },
            shape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp
        ) {
            panel()
        }

        // Left edge handle, always reachable while the panel is closed.
        if (progress < 0.98f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(18.dp)
                    .height(104.dp)
                    .graphicsLayer { alpha = 1f - progress }
                    .clickable(
                        enabled = progress < 0.1f,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpen()
                        }
                    )
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onDragEnd = {
                                if (dragged > 40f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpen()
                                }
                            },
                            onDragCancel = { dragged = 0f },
                            onHorizontalDrag = { _, amount -> dragged += amount }
                        )
                    },
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Small top-left label that names the landscape deck. */
@Composable
fun StreamDeckTitle(modifier: Modifier = Modifier) {
    Text(
        text = "stream deck",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(start = 24.dp, top = 16.dp)
    )
}

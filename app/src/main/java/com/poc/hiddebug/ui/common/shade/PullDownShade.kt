package com.poc.hiddebug.ui.common.shade

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scope passed to the dock and shade content.
 */
@Immutable
class ShadeScope internal constructor(
    val dragHandleModifier: Modifier,
    val progress: Float,
    private val onDismiss: () -> Unit,
    private val onOpen: () -> Unit
) {
    fun dismiss() = onDismiss()
    fun open() = onOpen()
}

/**
 * Notification-shade style container that morphs seamlessly from the status bar.
 *
 * Closed: Seamlessly integrated into the status bar area (starts at y=0, covers status bar,
 * status icons sit inside it, rounded bottom edge).
 *
 * Open: The exact same container surface morphs downwards with spring physics into the full-height
 * shade, revealing the snippets studio and bottom dock.
 */
@Composable
fun PullDownShade(
    dock: @Composable (scope: ShadeScope) -> Unit,
    shade: @Composable (scope: ShadeScope) -> Unit,
    modifier: Modifier = Modifier,
    dockHeight: Dp = 56.dp,
    openDockHeight: Dp = dockHeight,
    openHeight: Dp? = null,
    pushContentBelowShade: Boolean = false,
    floatBelowStatusBar: Boolean = false,
    horizontalInset: Dp = 0.dp,
    topGap: Dp = 8.dp,
    cornerRadius: Dp = 28.dp,
    drawBorder: Boolean = true,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val density = LocalDensity.current

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topOffset = if (floatBelowStatusBar) statusBarHeight + topGap else 0.dp
        val closedHeight = if (floatBelowStatusBar) {
            dockHeight
        } else {
            (statusBarHeight + dockHeight).coerceAtLeast(80.dp)
        }
        val minOpenHeight = closedHeight + 80.dp
        val availableHeight = if (floatBelowStatusBar) {
            maxHeight - topOffset - 16.dp
        } else {
            maxHeight - 16.dp
        }
        val maxOpenHeight = availableHeight.coerceAtLeast(minOpenHeight)
        val resolvedOpenHeight = (openHeight ?: maxOpenHeight).coerceIn(minOpenHeight, maxOpenHeight)

        val travelDistancePx = with(density) { (resolvedOpenHeight - closedHeight).toPx() }

        val targetProgress = if (isOpen) 1f else 0f
        val animatedProgress by animateFloatAsState(
            targetValue = if (travelDistancePx > 0f) {
                (targetProgress + dragOffset / travelDistancePx).coerceIn(0f, 1f)
            } else {
                targetProgress
            },
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "shadeProgress"
        )

        val currentHeight = closedHeight + (resolvedOpenHeight - closedHeight) * animatedProgress
        val currentDockHeight = dockHeight + (openDockHeight - dockHeight) * animatedProgress

        val shadeScope = remember(animatedProgress, isOpen) {
            ShadeScope(
                dragHandleModifier = Modifier,
                progress = animatedProgress,
                onDismiss = { isOpen = false },
                onOpen = { isOpen = true }
            )
        }

        val shadeBottom = topOffset + currentHeight
        val restingBottom = topOffset + closedHeight

        // Screen content can follow the shade down for compact, pushed layouts.
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(if (pushContentBelowShade) shadeBottom else restingBottom))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }

        // Dimming scrim when the shade expands
        if (animatedProgress > 0.01f && !pushContentBelowShade) {
            val scrimInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = animatedProgress * 0.65f }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = { isOpen = false }
                    )
            )
        }

        val shape = if (floatBelowStatusBar) {
            RoundedCornerShape(cornerRadius)
        } else {
            RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
        }
        val cardColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainer

        // Morphing surface. Attached shades grow out of the status bar.
        // Floating cards sit below it and keep the same rounded shape as they grow.
        Surface(
            modifier = Modifier
                .then(
                    if (floatBelowStatusBar) {
                        Modifier.padding(top = topOffset, start = horizontalInset, end = horizontalInset)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .height(currentHeight)
                .pointerInput(isOpen) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (isOpen) {
                                if (dragOffset < -80f) isOpen = false
                            } else {
                                if (dragOffset > 80f) isOpen = true
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                },
            shape = shape,
            color = cardColor,
            border = if (drawBorder) {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            } else {
                null
            },
            tonalElevation = 2.dp,
            shadowElevation = if (floatBelowStatusBar) 8.dp else (12 * animatedProgress).dp + 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Open shade body: smoothly fades in as the surface morphs downward
                if (animatedProgress > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = ((animatedProgress - 0.05f) / 0.95f).coerceIn(0f, 1f)
                            }
                    ) {
                        shade(shadeScope)
                    }
                }

                // Opaque backing for the dock strip. Without it the shade body keeps
                // scrolling visibly behind the dock controls once fully open, so the
                // backing fades in with the shade and disappears again on close.
                if (!pushContentBelowShade) {
                    val dockBackdropAlpha =
                        ((animatedProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    if (dockBackdropAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentDockHeight + 36.dp)
                                .align(Alignment.BottomCenter)
                                .graphicsLayer { alpha = dockBackdropAlpha }
                                .background(
                                    Brush.verticalGradient(
                                        0f to cardColor.copy(alpha = 0f),
                                        0.5f to cardColor,
                                        1f to cardColor
                                    )
                                )
                        )
                    }
                }

                // Morphing dock / crest: anchored at bottom edge of expanding surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentDockHeight)
                        .align(Alignment.BottomCenter)
                ) {
                    dock(shadeScope)
                }
            }
        }
    }
}

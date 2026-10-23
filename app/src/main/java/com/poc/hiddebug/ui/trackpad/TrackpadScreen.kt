package com.poc.hiddebug.ui.trackpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.hiddebug.domain.model.ConnectionStatus
import com.poc.hiddebug.domain.pointer.MouseButton
import com.poc.hiddebug.domain.pointer.PointerMath
import com.poc.hiddebug.domain.pointer.WheelAccumulator
import com.poc.hiddebug.service.HidService
import com.poc.hiddebug.ui.theme.MaterialShapes
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.launch

/**
 * Virtual trackpad.
 *
 * Touch model mirrors a hardware trackpad instead of exposing synthetic
 * buttons: drag moves the pointer, tap is a real left click, two quick taps
 * are a real double click. Every report is a plain HID press/release cycle,
 * so Windows and macOS both handle it natively.
 */
@Composable
fun TrackpadScreen(
    service: HidService?,
    config: TrackpadConfig = DefaultTrackpadConfig
) {
    val connectionState by service?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    val isLinked = connectionState == ConnectionStatus.CONNECTED
    val motion = remember(config) { PointerMotionState(config) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TrackpadHeader()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PointerPad(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                service = service,
                config = config,
                motion = motion,
                isLinked = isLinked
            )
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScrollStepKey(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    isUp = true,
                    enabled = isLinked,
                    onClick = {
                        service?.sendMouseMove(0, 0, wheel = config.scrollStepNotches)
                    }
                )
                ScrollRail(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    service = service,
                    config = config
                )
                ScrollStepKey(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    isUp = false,
                    enabled = isLinked,
                    onClick = {
                        service?.sendMouseMove(0, 0, wheel = -config.scrollStepNotches)
                    }
                )
            }
        }

        ClickDeck(
            enabled = isLinked,
            onLeftClick = { service?.sendMouseButton(MouseButton.LEFT) },
            onRightClick = { service?.sendMouseButton(MouseButton.RIGHT) }
        )
    }
}

// -----------------------------------------------------------------------------
// Header
// -----------------------------------------------------------------------------

@Composable
private fun TrackpadHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(MaterialShapes.ShapeClover4)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mouse,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(23.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Trackpad",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// -----------------------------------------------------------------------------
// Pointer surface
// -----------------------------------------------------------------------------

/**
 * Fractional motion accumulator. Keeping the remainder is what makes a slow
 * drag move the cursor at all after sensitivity is applied.
 */
private class PointerMotionState(private val config: TrackpadConfig) {
    private var remainderX = 0f
    private var remainderY = 0f

    fun reset() {
        remainderX = 0f
        remainderY = 0f
    }

    fun drag(dx: Float, dy: Float, emit: (Int, Int) -> Unit) {
        val stepX = PointerMath.accumulate(remainderX, dx, config.pointerSensitivity)
        remainderX = stepX.remainder
        val stepY = PointerMath.accumulate(remainderY, dy, config.pointerSensitivity)
        remainderY = stepY.remainder

        if (stepX.delta != 0 || stepY.delta != 0) {
            emit(stepX.delta, stepY.delta)
        }
    }
}

@Composable
private fun PointerPad(
    modifier: Modifier,
    service: HidService?,
    config: TrackpadConfig,
    motion: PointerMotionState,
    isLinked: Boolean
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var pressed by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf<Offset?>(null) }
    var rippleOrigin by remember { mutableStateOf(Offset.Zero) }
    val ripple = remember { Animatable(1f) }

    val idleAlpha by animateFloatAsState(
        targetValue = if (pressed) 0f else 1f,
        animationSpec = tween(180),
        label = "padIdleAlpha"
    )
    val glowStrength by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(120),
        label = "padGlow"
    )

    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.13f)
    val bracketColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (pressed) accent.copy(alpha = 0.75f) else bracketColor,
                shape = shape
            )
            .drawWithCache {
                val areaWidth = size.width
                val areaHeight = size.height
                val dotSpacing = 26.dp.toPx()
                val dots = buildList {
                    var x = dotSpacing / 2f
                    while (x < areaWidth) {
                        var y = dotSpacing / 2f
                        while (y < areaHeight) {
                            add(Offset(x, y))
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                val bracketInset = 14.dp.toPx()
                val bracketArm = 20.dp.toPx()
                val bracketStroke = 1.6.dp.toPx()
                onDrawBehind {
                    drawPoints(
                        points = dots,
                        pointMode = PointMode.Points,
                        color = dotColor,
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCornerBrackets(
                        color = bracketColor,
                        inset = bracketInset,
                        arm = bracketArm,
                        stroke = bracketStroke
                    )
                }
            }
            .trackpadGestures(
                service = service,
                config = config,
                motion = motion,
                onDown = { point ->
                    touch = point
                    rippleOrigin = point
                    pressed = true
                },
                onUp = {
                    pressed = false
                    touch = null
                },
                onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    service?.sendMouseButton(MouseButton.LEFT)
                    scope.launch {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, animationSpec = tween(420, easing = LinearEasing))
                    }
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTouchFeedback(
                point = touch,
                strength = glowStrength,
                color = accent
            )
            drawTapRipple(
                progress = ripple.value,
                origin = rippleOrigin,
                color = accent
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .alpha(idleAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(MaterialShapes.ShapeGhostish)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.9f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isLinked) "Drag to move" else "Not connected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}

private fun Modifier.trackpadGestures(
    service: HidService?,
    config: TrackpadConfig,
    motion: PointerMotionState,
    onDown: (Offset) -> Unit,
    onUp: () -> Unit,
    onTap: () -> Unit
): Modifier = pointerInput(service, config) {
    val dragThreshold = maxOf(viewConfiguration.touchSlop, config.dragStartDp.dp.toPx())

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        motion.reset()
        onDown(down.position)

        var previous = down.position
        var travel = 0f
        var dragging = false
        var released = false
        var lastUptime = down.uptimeMillis

        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                lastUptime = change.uptimeMillis

                if (!change.pressed && change.previousPressed) {
                    change.consume()
                    released = true
                    break
                }

                val dx = change.position.x - previous.x
                val dy = change.position.y - previous.y
                previous = change.position
                travel += hypot(dx, dy)

                if (!dragging && travel >= dragThreshold) dragging = true

                if (dragging) {
                    change.consume()
                    motion.drag(dx, dy) { stepX, stepY -> service?.sendMouseMove(stepX, stepY) }
                    onDown(change.position)
                }
            }
        } finally {
            onUp()
        }

        val wasTap = released && !dragging && lastUptime - down.uptimeMillis <= config.tapMaxDurationMs
        if (wasTap) onTap()
    }
}

private fun DrawScope.drawCornerBrackets(color: Color, inset: Float, arm: Float, stroke: Float) {
    val corners = listOf(
        Triple(Offset(inset, inset), 1f, 1f),
        Triple(Offset(size.width - inset, inset), -1f, 1f),
        Triple(Offset(inset, size.height - inset), 1f, -1f),
        Triple(Offset(size.width - inset, size.height - inset), -1f, -1f)
    )
    corners.forEach { (origin, directionX, directionY) ->
        drawLine(
            color = color,
            start = origin,
            end = Offset(origin.x + arm * directionX, origin.y),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = origin,
            end = Offset(origin.x, origin.y + arm * directionY),
            strokeWidth = stroke
        )
    }
}

private fun DrawScope.drawTouchFeedback(
    point: Offset?,
    strength: Float,
    color: Color
) {
    if (point == null || strength <= 0.01f) return

    val radius = 150.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.26f * strength), Color.Transparent),
            center = point,
            radius = radius
        ),
        radius = radius,
        center = point
    )
    drawCircle(color = color.copy(alpha = 0.4f * strength), radius = 14.dp.toPx(), center = point)
    drawCircle(
        color = color.copy(alpha = 0.95f * strength),
        radius = 5.5.dp.toPx(),
        center = point
    )
}

private fun DrawScope.drawTapRipple(
    progress: Float,
    origin: Offset,
    color: Color
) {
    if (progress >= 1f) return

    val eased = FastOutSlowInEasing.transform(progress)
    drawCircle(
        color = color.copy(alpha = (1f - progress.coerceIn(0f, 1f)) * 0.55f),
        radius = 10.dp.toPx() + 42.dp.toPx() * eased,
        center = origin,
        style = Stroke(width = 2.dp.toPx())
    )
}

// -----------------------------------------------------------------------------
// Scroll rail
// -----------------------------------------------------------------------------

@Composable
private fun ScrollRail(modifier: Modifier, service: HidService?, config: TrackpadConfig) {
    val haptics = LocalHapticFeedback.current
    var dragY by remember { mutableStateOf<Float?>(null) }

    val shape = RoundedCornerShape(14.dp)
    val accent = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), shape)
            .pointerInput(service, config) {
                val accumulator = WheelAccumulator(config.scrollStepDp.dp.toPx())
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        accumulator.reset()
                        dragY = offset.y
                    },
                    onDragEnd = {
                        dragY = null
                        accumulator.reset()
                    },
                    onDragCancel = {
                        dragY = null
                        accumulator.reset()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragY = change.position.y
                        val notches = accumulator.feed(dragAmount)
                        if (notches != 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            service?.sendMouseMove(0, 0, wheel = notches)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScrollTicks(tickColor = tickColor, activeY = dragY)
            dragY?.let { y -> drawScrollThumb(centerY = y, color = accent) }
        }

        Text(
            text = "SCROLL",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(horizontal = 5.dp, vertical = 3.dp)
        )
    }
}

private fun DrawScope.drawScrollTicks(tickColor: Color, activeY: Float?) {
    val step = 15.dp.toPx()
    val inset = 20.dp.toPx()
    val influence = (size.height * 0.22f).coerceAtLeast(1f)

    var y = step
    while (y < size.height - step / 2f) {
        val proximity = if (activeY == null) 0f else (1f - abs(y - activeY) / influence).coerceIn(0f, 1f)
        drawLine(
            color = tickColor.copy(alpha = 0.28f + 0.5f * proximity),
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 1.dp.toPx()
        )
        y += step
    }
}

private fun DrawScope.drawScrollThumb(centerY: Float, color: Color) {
    val height = 46.dp.toPx()
    val top = (centerY - height / 2f).coerceIn(4.dp.toPx(), size.height - height - 4.dp.toPx())

    drawRoundRect(
        color = color.copy(alpha = 0.16f),
        topLeft = Offset(6.dp.toPx(), top),
        size = Size(size.width - 12.dp.toPx(), height),
        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
    )
    drawRoundRect(
        color = color.copy(alpha = 0.55f),
        topLeft = Offset(6.dp.toPx(), top),
        size = Size(size.width - 12.dp.toPx(), height),
        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

// -----------------------------------------------------------------------------
// Fixed scroll keys
// -----------------------------------------------------------------------------

/**
 * Detached M3 Expressive arch cap sitting at either end of the rail. One tap
 * sends a fixed wheel step, the same action the CLI's `scroll-up` /
 * `scroll-down` tokens produce, just in a preset amount
 * ([TrackpadConfig.scrollStepNotches]).
 *
 * The arch keeps its catalog orientation at the top and is mirrored at the
 * bottom so both caps face away from the rail. The press uses a Material 3
 * Expressive spring: the cap squashes on touch-down and springs back.
 */
@Composable
private fun ScrollStepKey(
    modifier: Modifier,
    isUp: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val nudge = remember { Animatable(0f) }
    val direction = if (isUp) 1f else -1f

    val scaleX by animateFloatAsState(
        targetValue = if (pressed) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.32f, stiffness = 900f),
        label = "scrollStepScaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.30f, stiffness = 850f),
        label = "scrollStepScaleY"
    )
    val arrowOffset by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 900f),
        label = "scrollStepArrow"
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .scale(scaleX, scaleY)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Material 3 Expressive confirmation: the arrow follows the
                    // scroll step, then springs back.
                    scope.launch {
                        nudge.snapTo(0f)
                        nudge.animateTo(1f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                        nudge.animateTo(0f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f))
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shape only, mirrored at the bottom so both caps point away from the
        // rail while the arrow itself stays upright.
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotate(if (isUp) 0f else 180f)
                .clip(MaterialShapes.ShapeArch)
                .background(MaterialTheme.colorScheme.primary)
        )

        Icon(
            imageVector = if (isUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isUp) "Scroll up" else "Scroll down",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .offset(y = (arrowOffset + (nudge.value * 4f).dp) * direction)
                .size(22.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// Click deck
// -----------------------------------------------------------------------------

@Composable
private fun ClickDeck(enabled: Boolean, onLeftClick: () -> Unit, onRightClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PointerActionKey(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "Left",
            isRightButton = false,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLeftClick()
            }
        )
        PointerActionKey(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "Right",
            isRightButton = true,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRightClick()
            }
        )
    }
}

@Composable
private fun PointerActionKey(
    modifier: Modifier,
    label: String,
    isRightButton: Boolean,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "keyScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.86f)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MouseButtonGlyph(isRightButton = isRightButton, color = contentColor)
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Miniature mouse outline with the relevant half shaded. */
@Composable
private fun MouseButtonGlyph(isRightButton: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 1.6.dp.toPx()
        val corner = CornerRadius(5.5.dp.toPx(), 5.5.dp.toPx())
        val split = size.height * 0.44f

        drawRoundRect(
            color = color.copy(alpha = 0.9f),
            cornerRadius = corner,
            style = Stroke(width = stroke)
        )
        clipRect(
            left = if (isRightButton) size.width / 2f else 0f,
            right = if (isRightButton) size.width else size.width / 2f,
            top = 0f,
            bottom = split
        ) {
            drawRoundRect(
                color = color.copy(alpha = 0.38f),
                cornerRadius = corner
            )
        }
        drawLine(
            color = color.copy(alpha = 0.65f),
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, split),
            strokeWidth = stroke
        )
    }
}

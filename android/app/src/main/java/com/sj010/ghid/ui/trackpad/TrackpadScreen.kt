package com.sj010.ghid.ui.trackpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.pointer.MouseButton
import com.sj010.ghid.domain.pointer.PointerMath
import com.sj010.ghid.domain.pointer.WheelAccumulator
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.MaterialShapes
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Virtual trackpad with notification-style Gestures pull-down shade.
 *
 * Closed: Seamlessly integrated status bar with "gestures" title and drag handle.
 * When pulled down: reveals the full Gestures Gallery (3-finger & 4-finger precision shortcuts,
 * practice pad, and OS toggles).
 * On-pad: The clean, full-size virtual trackpad detects 1-finger pointer motion, taps,
 * scroll rail inputs, click deck, AND 3/4-finger live swipes.
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
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var targetOs by remember { mutableStateOf(TargetOs.WINDOWS) }
    var activeGestureHud by remember { mutableStateOf<TrackpadGesture?>(null) }

    val handleTriggerGesture: (TrackpadGesture) -> Unit = { gesture ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val combo = gesture.comboFor(targetOs)
        service?.executeCombo(combo)
        activeGestureHud = gesture
        scope.launch {
            delay(2200)
            if (activeGestureHud == gesture) {
                activeGestureHud = null
            }
        }
    }

    GesturesShade(
        service = service
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Pointer Pad & Scroll Rail
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
                    isLinked = isLinked,
                    activeGestureHud = activeGestureHud,
                    targetOs = targetOs,
                    onMultiFingerGesture = handleTriggerGesture
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

            // Mouse Click Deck
            ClickDeck(
                enabled = isLinked,
                onLeftClick = { service?.sendMouseButton(MouseButton.LEFT) },
                onRightClick = { service?.sendMouseButton(MouseButton.RIGHT) }
            )
        }
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
    isLinked: Boolean,
    activeGestureHud: TrackpadGesture?,
    targetOs: TargetOs,
    onMultiFingerGesture: (TrackpadGesture) -> Unit
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
                    drawCornerBrackets(bracketColor, bracketInset, bracketArm, bracketStroke)
                }
            }
            .trackpadGestures(
                service = service,
                motion = motion,
                config = config,
                isLinked = isLinked,
                targetOs = targetOs,
                onPressedChange = { pressed = it },
                onTouchChange = { touch = it },
                onTapRipple = { pos ->
                    rippleOrigin = pos
                    scope.launch {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
                    }
                },
                onMultiFingerGesture = onMultiFingerGesture
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTouchFeedback(touch, glowStrength, accent)
            drawTapRipple(ripple.value, rippleOrigin, accent)
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
                Icon(Icons.Default.Mouse, contentDescription = null, tint = accent.copy(alpha = 0.9f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (isLinked) "Drag to move" else "Not connected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }

        // Animated Gesture HUD Overlay
        AnimatedVisibility(
            visible = activeGestureHud != null,
            enter = scaleIn(spring(stiffness = 500f)) + fadeIn(),
            exit = scaleOut(spring(stiffness = 500f)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
        ) {
            activeGestureHud?.let { gesture ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(gesture.resolveShape())
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = gesture.resolveIcon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "${if (gesture.fingers == FingerCount.THREE) "3" else "4"}-Finger ${gesture.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = gesture.comboFor(targetOs),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles pointer events on the trackpad.
 * Supports:
 * - 1-finger drag: relative cursor motion
 * - 1-finger tap: left click
 * - 1-finger double tap: double left click
 * - 3-finger & 4-finger swipes (Up, Down, Left, Right)
 */
private fun Modifier.trackpadGestures(
    service: HidService?,
    motion: PointerMotionState,
    config: TrackpadConfig,
    isLinked: Boolean,
    targetOs: TargetOs,
    onPressedChange: (Boolean) -> Unit,
    onTouchChange: (Offset?) -> Unit,
    onTapRipple: (Offset) -> Unit,
    onMultiFingerGesture: (TrackpadGesture) -> Unit
): Modifier = pointerInput(isLinked, config, targetOs) {
    val tapTimeoutMs = config.tapMaxDurationMs
    val doubleTapTimeoutMs = 300L
    val slopPx = 10.dp.toPx()
    val gestureThresholdPx = 45.dp.toPx()

    var lastTapUpTime = 0L
    var lastTapPos = Offset.Zero

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPressedChange(true)
        onTouchChange(down.position)
        motion.reset()

        val downTime = System.currentTimeMillis()
        var moved = false
        var lastPos = down.position
        val pointerId = down.id

        // Multi-touch tracking
        var multiTouchFired = false
        val initialPointers = mutableMapOf<Long, Offset>()
        initialPointers[down.id.value] = down.position

        while (true) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }

            if (pressedChanges.isEmpty()) {
                onPressedChange(false)
                onTouchChange(null)

                val upTime = System.currentTimeMillis()
                val duration = upTime - downTime
                val totalDist = (lastPos - down.position).getDistance()

                if (!moved && !multiTouchFired && duration <= tapTimeoutMs && totalDist <= slopPx && isLinked) {
                    val isDouble = (upTime - lastTapUpTime) <= doubleTapTimeoutMs &&
                        (lastPos - lastTapPos).getDistance() <= slopPx * 2f

                    onTapRipple(lastPos)
                    if (isDouble) {
                        service?.sendMouseButton(MouseButton.LEFT)
                        service?.sendMouseButton(MouseButton.LEFT)
                        lastTapUpTime = 0L
                    } else {
                        service?.sendMouseButton(MouseButton.LEFT)
                        lastTapUpTime = upTime
                        lastTapPos = lastPos
                    }
                }
                break
            }

            // Record multi-touch points
            pressedChanges.forEach { change ->
                if (!initialPointers.containsKey(change.id.value)) {
                    initialPointers[change.id.value] = change.position
                }
            }

            // Check for 3-finger or 4-finger swipes
            if (!multiTouchFired && initialPointers.size in 3..4 && pressedChanges.size in 3..4) {
                var dxSum = 0f
                var dySum = 0f
                var validCount = 0

                pressedChanges.forEach { change ->
                    val origin = initialPointers[change.id.value]
                    if (origin != null) {
                        dxSum += (change.position.x - origin.x)
                        dySum += (change.position.y - origin.y)
                        validCount++
                    }
                }

                if (validCount >= 3) {
                    val dxAvg = dxSum / validCount
                    val dyAvg = dySum / validCount
                    val distance = hypot(dxAvg, dyAvg)

                    if (distance >= gestureThresholdPx) {
                        val fingerCount = if (validCount >= 4) FingerCount.FOUR else FingerCount.THREE
                        val direction = if (abs(dyAvg) > abs(dxAvg)) {
                            if (dyAvg < 0) GestureDirection.UP else GestureDirection.DOWN
                        } else {
                            if (dxAvg < 0) GestureDirection.LEFT else GestureDirection.RIGHT
                        }

                        val gesture = TrackpadGestureRegistry.find(fingerCount, direction)
                        if (gesture != null) {
                            multiTouchFired = true
                            onMultiFingerGesture(gesture)
                            pressedChanges.forEach { it.consume() }
                        }
                    }
                }
            }

            // 1-finger relative mouse cursor movement
            if (!multiTouchFired) {
                val primaryChange = pressedChanges.firstOrNull { it.id == pointerId } ?: pressedChanges.first()
                val delta = primaryChange.position - lastPos
                lastPos = primaryChange.position
                onTouchChange(primaryChange.position)

                if (delta.getDistance() > 0.5f) {
                    moved = true
                    if (isLinked) {
                        motion.drag(delta.x, delta.y) { dx, dy ->
                            service?.sendMouseMove(dx, dy)
                        }
                    }
                    primaryChange.consume()
                }
            }
        }
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
// Stepped scroll buttons & continuous rail
// -----------------------------------------------------------------------------

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
// Mouse Click Deck
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

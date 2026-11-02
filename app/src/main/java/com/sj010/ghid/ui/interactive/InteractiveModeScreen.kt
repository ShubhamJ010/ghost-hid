package com.sj010.ghid.ui.interactive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.R
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.pointer.MouseButton
import com.sj010.ghid.domain.pointer.PointerMath
import com.sj010.ghid.service.HidService

/**
 * Interactive mode: the phone's own keyboard becomes the Bluetooth keyboard of
 * the connected device.
 *
 * Every soft-keyboard press is forwarded immediately. The keyboard island
 * chains sticky modifiers and quick keys, while a separate preview shows
 * armed modifiers and recently sent keys. The key drawer contains modifier,
 * navigation, media and function keys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveModeScreen(service: HidService?, onExit: () -> Unit) {
    val connectionState by service?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    val isLinked = connectionState == ConnectionStatus.CONNECTED
    val detectedOs by service?.effectiveTargetOs?.collectAsState()
        ?: remember { mutableStateOf(TargetOs.WINDOWS) }

    val settingsRepository = remember { GhostHidApp.instance.settingsRepository }
    val settings by settingsRepository.settings.collectAsState()
    val mapperKeys = settings.interactiveMapperKeys
    val trackpadSensitivity = settings.trackpadPointerSensitivity

    var heldModifiers by remember { mutableStateOf(emptySet<InteractiveModifier>()) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var isKeyDrawerOpen by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var assigningSlotIndex by remember { mutableStateOf<Int?>(null) }

    // The capture field is anchored with an invisible newline so a backspace is
    // always a real deletion; without it the empty field would swallow it.
    var captureValue by remember { mutableStateOf(sentinelValue()) }

    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val captureFocus = remember { FocusRequester() }

    val focusCaptureField: () -> Unit = {
        captureFocus.requestFocus()
        keyboardController?.show()
    }

    val imeVisible = WindowInsets.isImeVisible

    fun log(keyLabel: String, modifiers: Set<InteractiveModifier>) {
        val prefix = InteractiveKeys.modifierLabel(modifiers, detectedOs)
        val entry = if (prefix == null) keyLabel else "$prefix + $keyLabel"
        history = (listOf(entry) + history).take(8)
        heldModifiers = emptySet()
    }

    val sendCatalogKey: (InteractiveKey) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        InteractiveKeys.send(service, key, heldModifiers)
        log(key.label, heldModifiers)
    }

    val updateSlot: (Int, String) -> Unit = { index, token ->
        val current = mapperKeys.toMutableList()
        while (current.size < 4) {
            current.add("")
        }
        if (index in current.indices) {
            current[index] = token
        }
        settingsRepository.update { it.copy(interactiveMapperKeys = current) }
    }

    // Dismiss keyboard when drawer opens or when exiting
    LaunchedEffect(isKeyDrawerOpen, isExiting) {
        if (isExiting || isKeyDrawerOpen) {
            keyboardController?.hide()
        }
    }

    val exitMode: () -> Unit = {
        isExiting = true
        keyboardController?.hide()
        onExit()
    }

    BackHandler(enabled = !isExiting) {
        if (imeVisible) {
            keyboardController?.hide()
        } else {
            exitMode()
        }
    }

    InteractiveKeyShade(
        targetOs = detectedOs,
        heldModifiers = heldModifiers,
        onKey = sendCatalogKey,
        onToggleModifier = { modifier ->
            heldModifiers = if (modifier in heldModifiers) {
                heldModifiers - modifier
            } else {
                heldModifiers + modifier
            }
        },
        onOpenChanged = { isKeyDrawerOpen = it }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedContent(
                    targetState = imeVisible,
                    transitionSpec = {
                        if (targetState) {
                            (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(220, delayMillis = 40)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(150)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(150))
                                )
                        } else {
                            (fadeIn(animationSpec = tween(280, delayMillis = 60)) +
                                scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(150)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(150))
                                )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                    label = "interactiveStageTransition"
                ) { isKeyboardOpen ->
                    if (isKeyboardOpen) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Center stage: ghost floating above the phone keyboard
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_ghost_keyboard_mode),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Text(
                                        text = "Interactive mode",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isLinked) {
                                            "Every key you type is sent to the device."
                                        } else {
                                            "Not connected. Connect to a device to type through."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Preview card (shows "Press a modifier..." when keyboard is open)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Preview",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        StatusPill(isLinked = isLinked)
                                    }
                                    InteractivePreviewChips(
                                        heldModifiers = heldModifiers,
                                        history = history,
                                        targetOs = detectedOs,
                                        showPlaceholder = true
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Minimal top header row when keyboard is dismissed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_ghost_keyboard_mode),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Interactive mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                StatusPill(isLinked = isLinked)
                            }

                            // Compact preview card: only displayed when there are active modifiers or history
                            if (heldModifiers.isNotEmpty() || history.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        InteractivePreviewChips(
                                            heldModifiers = heldModifiers,
                                            history = history,
                                            targetOs = detectedOs,
                                            showPlaceholder = false
                                        )
                                    }
                                }
                            }

                            // Square trackpad with wide right click
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                SquareTrackpad(
                                    service = service,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                                    isLinked = isLinked,
                                    sensitivity = trackpadSensitivity
                                )
                            }
                        }
                    }
                }

                // Keyboard island deck at the bottom (shared across keyboard open/closed)
                KeyboardIsland(
                    detectedOs = detectedOs,
                    heldModifiers = heldModifiers,
                    mapperKeys = mapperKeys,
                    onToggleModifier = { modifier ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        heldModifiers = if (modifier in heldModifiers) {
                            heldModifiers - modifier
                        } else {
                            heldModifiers + modifier
                        }
                    },
                    onKeyClick = sendCatalogKey,
                    onSlotHold = { slotIndex -> assigningSlotIndex = slotIndex },
                    onSlotAdd = { slotIndex -> assigningSlotIndex = slotIndex },
                    onIslandClick = focusCaptureField
                )
            }

            // Invisible capture field: the system IME stays attached to it while
            // every diff is forwarded over HID.
            BasicTextField(
                value = captureValue,
                onValueChange = { newValue ->
                    val oldText = captureValue.text
                    val newText = newValue.text
                    when {
                        newText.startsWith(oldText) -> {
                            newText.substring(oldText.length).forEach { char ->
                                if (InteractiveKeys.sendChar(service, char, heldModifiers)) {
                                    log(labelForChar(char), heldModifiers)
                                }
                            }
                        }

                        oldText.startsWith(newText) -> {
                            repeat(oldText.length - newText.length) {
                                InteractiveKeys.sendBackspace(service)
                                history = (listOf("⌫") + history).take(8)
                            }
                        }

                        else -> {
                            // The IME replaced the whole field instead of editing it.
                            newText.removePrefix(CAPTURE_SENTINEL).forEach { char ->
                                if (InteractiveKeys.sendChar(service, char, heldModifiers)) {
                                    log(labelForChar(char), heldModifiers)
                                }
                            }
                        }
                    }
                    // Re-anchor at the end so every keystroke is a fresh append.
                    captureValue = sentinelValue()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(captureFocus),
                textStyle = MaterialTheme.typography.bodySmall,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None
                )
            )

            // Key assignment bottom sheet
            if (assigningSlotIndex != null) {
                val slotIdx = assigningSlotIndex!!
                val token = mapperKeys.getOrElse(slotIdx) { "" }
                val currentKey = InteractiveKeys.findByToken(token)
                KeyPickerSheet(
                    slotIndex = slotIdx,
                    currentKey = currentKey,
                    onSelectKey = { pickedKey ->
                        updateSlot(slotIdx, pickedKey.token)
                        assigningSlotIndex = null
                    },
                    onClearKey = {
                        updateSlot(slotIdx, "")
                        assigningSlotIndex = null
                    },
                    onDismiss = { assigningSlotIndex = null }
                )
            }
        }
    }
}

private const val CAPTURE_SENTINEL = "\n"

private fun sentinelValue() = TextFieldValue(
    text = CAPTURE_SENTINEL,
    selection = TextRange(CAPTURE_SENTINEL.length)
)

private fun labelForChar(char: Char): String = when (char) {
    ' ' -> "Space"
    '\n', '\r' -> "Enter"
    '\t' -> "Tab"
    else -> char.toString()
}

@Composable
private fun StatusPill(isLinked: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isLinked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = if (isLinked) "CONNECTED" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = if (isLinked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractivePreviewChips(
    heldModifiers: Set<InteractiveModifier>,
    history: List<String>,
    targetOs: TargetOs,
    showPlaceholder: Boolean = true
) {
    if (heldModifiers.isEmpty() && history.isEmpty()) {
        if (showPlaceholder) {
            Text(
                text = "Press a modifier to chain it, then any key is sent straight to the device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        heldModifiers.forEach { modifier ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = modifier.labelFor(targetOs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        history.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = entry,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ModifierKeyChip(
    modifier: Modifier,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 8.dp,
            bottomEnd = 14.dp,
            bottomStart = 8.dp
        ),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Keyboard island deck with modifiers, 4 mapper keys, and center-aligned touch instruction.
 */
@Composable
private fun KeyboardIsland(
    detectedOs: TargetOs,
    heldModifiers: Set<InteractiveModifier>,
    mapperKeys: List<String>,
    onToggleModifier: (InteractiveModifier) -> Unit,
    onKeyClick: (InteractiveKey) -> Unit,
    onSlotHold: (Int) -> Unit,
    onSlotAdd: (Int) -> Unit,
    onIslandClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIslandClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Modifier keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                InteractiveKeys.modifiers.forEach { modifier ->
                    ModifierKeyChip(
                        modifier = Modifier.weight(1f),
                        label = modifier.labelFor(detectedOs),
                        active = modifier in heldModifiers,
                        onClick = { onToggleModifier(modifier) }
                    )
                }
            }

            // Row 2: 4 mapper buttons (defaults: Esc, Tab, Stab, dotted +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0 until 4).forEach { slotIndex ->
                    val token = mapperKeys.getOrElse(slotIndex) { "" }
                    val key = InteractiveKeys.findByToken(token)
                    if (key != null) {
                        MapperKeyButton(
                            modifier = Modifier.weight(1f),
                            key = key,
                            onClick = { onKeyClick(key) },
                            onLongClick = { onSlotHold(slotIndex) }
                        )
                    } else {
                        DottedAddKey(
                            modifier = Modifier.weight(1f),
                            onClick = { onSlotAdd(slotIndex) }
                        )
                    }
                }
            }

            // Center-aligned text to touch island and open keyboard
            Text(
                text = "Touch this island to open keyboard",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Square trackpad with touch motion and minimal Point & Right Click buttons.
 */
private class InteractivePointerMotionState(private val sensitivity: Float) {
    private var remainderX = 0f
    private var remainderY = 0f

    fun reset() {
        remainderX = 0f
        remainderY = 0f
    }

    fun drag(dx: Float, dy: Float, emit: (Int, Int) -> Unit) {
        val stepX = PointerMath.accumulate(remainderX, dx, sensitivity)
        remainderX = stepX.remainder
        val stepY = PointerMath.accumulate(remainderY, dy, sensitivity)
        remainderY = stepY.remainder

        if (stepX.delta != 0 || stepY.delta != 0) {
            emit(stepX.delta, stepY.delta)
        }
    }
}

@Composable
fun SquareTrackpad(
    service: HidService?,
    modifier: Modifier = Modifier,
    isLinked: Boolean = true,
    sensitivity: Float = 1.5f
) {
    val haptics = LocalHapticFeedback.current
    val motion = remember(sensitivity) { InteractivePointerMotionState(sensitivity) }
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Touch pointing area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(isLinked, sensitivity) {
                        val tapTimeoutMs = 300L
                        val slopPx = 10.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            touchPos = down.position
                            motion.reset()

                            val downTime = System.currentTimeMillis()
                            var moved = false
                            var lastPos = down.position
                            val pointerId = down.id
                            var maxPointers = 1

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }
                                if (pressedChanges.size > maxPointers) {
                                    maxPointers = pressedChanges.size
                                }

                                if (pressedChanges.isEmpty()) {
                                    touchPos = null
                                    val upTime = System.currentTimeMillis()
                                    val duration = upTime - downTime
                                    val totalDist = (lastPos - down.position).getDistance()

                                    if (!moved && duration <= tapTimeoutMs && totalDist <= slopPx && isLinked) {
                                        if (maxPointers >= 2) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            service?.sendMouseButton(MouseButton.RIGHT)
                                        } else {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            service?.sendMouseButton(MouseButton.LEFT)
                                        }
                                    }
                                    break
                                }

                                val primary = pressedChanges.firstOrNull { it.id == pointerId }
                                    ?: pressedChanges.first()
                                val delta = primary.position - lastPos
                                lastPos = primary.position
                                touchPos = primary.position

                                if (delta.getDistance() > 0.5f) {
                                    moved = true
                                    if (isLinked) {
                                        motion.drag(delta.x, delta.y) { dx, dy ->
                                            service?.sendMouseMove(dx, dy)
                                        }
                                    }
                                    primary.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    touchPos?.let { pos ->
                        drawCircle(
                            color = accent.copy(alpha = 0.2f),
                            radius = 28.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.6f),
                            radius = 10.dp.toPx(),
                            center = pos
                        )
                    }
                }

                if (touchPos == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        if (!isLinked) {
                            Text(
                                text = "Not connected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Wide Right Click button
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    service?.sendMouseButton(MouseButton.RIGHT)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Right Click",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Dotted '+' button for unassigned mapper slots. Click to add/assign key.
 */
@Composable
private fun DottedAddKey(
    modifier: Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawRoundRect(
                    color = outlineColor,
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add key",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Minimal button displaying an assigned mapper key. Click to send, hold to replace.
 * Kept strictly minimal without subtext.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapperKeyButton(
    modifier: Modifier,
    key: InteractiveKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(42.dp)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Key picker bottom sheet to assign or replace a key for a mapper slot.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun KeyPickerSheet(
    slotIndex: Int,
    currentKey: InteractiveKey?,
    onSelectKey: (InteractiveKey) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Assign Key (Slot ${slotIndex + 1})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentKey != null) {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearKey()
                    }) {
                        Text("Reset to +", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                InteractiveKeyCategory.entries.forEach { category ->
                    item(key = category.name) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InteractiveKeys.keysIn(category).forEach { key ->
                                    val isSelected = key.token == currentKey?.token
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectKey(key)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = key.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

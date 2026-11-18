package com.sj010.ghid.ui.interactive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.MainActivity
import com.sj010.ghid.domain.model.InteractiveVolumeKeyMode
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.service.HidService
import kotlinx.coroutines.delay

/**
 * POC: interactive mode as a transparent overlay on the dashboard.
 *
 * Nothing here paints a scrim or a page background, so the dashboard stays
 * visible and touchable around the island. The keys and the preview popup own
 * the bottom edge while the dashboard's device card shrinks away underneath,
 * and [onDismiss] (back or the overlay's own close) morphs the keys back into
 * that card.
 *
 * Deliberately absent compared to the full screen: the title/subtitle block and
 * the square trackpad gesture pad.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveOverlay(
    service: HidService?,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsRepository = remember { GhostHidApp.instance.settingsRepository }
    val settings by settingsRepository.settings.collectAsState()
    val mapperKeys = settings.interactiveMapperKeys
    val detectedOs by service?.effectiveTargetOs?.collectAsState()
        ?: remember { mutableStateOf(TargetOs.WINDOWS) }

    var heldModifiers by remember { mutableStateOf(emptySet<InteractiveModifier>()) }
    var history by remember { mutableStateOf(emptyList<PreviewItem>()) }
    var lastTypeTime by remember { mutableStateOf(0L) }
    var assigningSlotIndex by remember { mutableStateOf<Int?>(null) }
    var isKeyDrawerOpen by remember { mutableStateOf(false) }
    var captureValue by remember { mutableStateOf(overlaySentinelValue()) }

    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val captureFocus = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible
    var keyboardWasOpen by remember { mutableStateOf(false) }
    // Set while the overlay itself hides the IME (drawer), so that hide is not read as
    // the user putting the keyboard away.
    var suppressingImeClose by remember { mutableStateOf(false) }

    val focusCaptureField: () -> Unit = {
        captureFocus.requestFocus()
        keyboardController?.show()
    }

    fun pushHistory(item: PreviewItem) {
        history = (listOf(item) + history).take(OVERLAY_HISTORY_ITEMS)
    }

    // The chips read as one entry per key, so a repeat replaces the newest one.
    fun replaceNewest(item: PreviewItem) {
        history = listOf(item) + history.drop(1)
    }

    fun logKey(label: String, modifiers: Set<InteractiveModifier>) {
        val prefix = InteractiveKeys.modifierLabel(modifiers, detectedOs)
        val now = android.os.SystemClock.uptimeMillis()
        val first = history.firstOrNull()
        if (first is PreviewItem.KeyPress && first.label == label && first.modifierPrefix == prefix) {
            replaceNewest(first.copy(count = first.count + 1, timestamp = now))
        } else {
            pushHistory(PreviewItem.KeyPress(label = label, modifierPrefix = prefix, timestamp = now))
        }
        heldModifiers = emptySet()
    }

    fun onCharTyped(char: Char) {
        InteractiveKeys.sendChar(service, char, heldModifiers)

        if (heldModifiers.isNotEmpty()) {
            logKey(labelForOverlayChar(char), heldModifiers)
            return
        }

        if (char == '\n' || char == '\r') {
            logKey("Enter", emptySet())
            return
        }
        if (char == '\t') {
            logKey("Tab", emptySet())
            return
        }

        val now = android.os.SystemClock.uptimeMillis()
        val isWithinCooldown = (now - lastTypeTime) <= OVERLAY_TYPING_COOLDOWN_MS
        lastTypeTime = now

        val first = history.firstOrNull()
        if (isWithinCooldown && first is PreviewItem.TextChunk) {
            replaceNewest(first.copy(text = first.text + char, timestamp = now))
        } else {
            pushHistory(PreviewItem.TextChunk(text = char.toString(), timestamp = now))
        }
        heldModifiers = emptySet()
    }

    fun onBackspace() {
        InteractiveKeys.sendBackspace(service)
        val now = android.os.SystemClock.uptimeMillis()
        val first = history.firstOrNull()
        val isWithinCooldown = (now - lastTypeTime) <= OVERLAY_TYPING_COOLDOWN_MS
        if (isWithinCooldown && first is PreviewItem.TextChunk && first.text.isNotEmpty()) {
            lastTypeTime = now
            val shortened = first.text.dropLast(1)
            history = if (shortened.isEmpty()) {
                history.drop(1)
            } else {
                listOf(first.copy(text = shortened, timestamp = now)) + history.drop(1)
            }
        } else {
            logKey("Backspace", emptySet())
        }
    }

    val sendCatalogKey: (InteractiveKey) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        InteractiveKeys.send(service, key, heldModifiers)
        logKey(key.label, heldModifiers)
    }

    val toggleModifier: (InteractiveModifier) -> Unit = { modifier ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        heldModifiers = if (modifier in heldModifiers) {
            heldModifiers - modifier
        } else {
            heldModifiers + modifier
        }
    }

    val currentSendCatalogKey by rememberUpdatedState(sendCatalogKey)
    val currentVolumeMode by rememberUpdatedState(settings.interactiveVolumeKeyMode)

    // Volume buttons drive the overlay only while it is on screen.
    DisposableEffect(visible) {
        if (visible) {
            var lastTime = 0L
            MainActivity.volumeKeyHandler = { isUp, repeatCount ->
                val now = android.os.SystemClock.uptimeMillis()
                if (repeatCount == 0 || (now - lastTime) >= 120L) {
                    lastTime = now
                    val key = when (currentVolumeMode) {
                        InteractiveVolumeKeyMode.VOLUME ->
                            if (isUp) InteractiveKeys.findByToken("volumeup") else InteractiveKeys.findByToken("volumedown")
                        InteractiveVolumeKeyMode.ARROWS ->
                            if (isUp) InteractiveKeys.findByToken("right") else InteractiveKeys.findByToken("left")
                        InteractiveVolumeKeyMode.TRACKS ->
                            if (isUp) InteractiveKeys.findByToken("nexttrack") else InteractiveKeys.findByToken("previoustrack")
                    }
                    if (key != null) {
                        currentSendCatalogKey(key)
                    }
                }
            }
        }
        onDispose {
            MainActivity.volumeKeyHandler = null
        }
    }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            delay(4000L)
            history = emptyList()
        }
    }

    // The keyboard comes up with the overlay: this mode exists to type through.
    LaunchedEffect(visible) {
        if (visible) {
            delay(180)
            focusCaptureField()
        } else {
            keyboardController?.hide()
            heldModifiers = emptySet()
            history = emptyList()
            assigningSlotIndex = null
        }
    }

    // The full key drawer needs the bottom of the screen, so it puts the IME away and
    // brings it back when it closes again.
    LaunchedEffect(isKeyDrawerOpen) {
        if (!visible) return@LaunchedEffect
        if (isKeyDrawerOpen) {
            suppressingImeClose = true
            keyboardController?.hide()
        } else {
            focusCaptureField()
        }
    }

    // Putting the keyboard away is how you leave this mode: the overlay animates out
    // with it. This is keyed on the IME alone, so closing the drawer (which reopens the
    // keyboard) and the key picker never race the dismissal.
    LaunchedEffect(imeVisible) {
        if (!visible) {
            keyboardWasOpen = false
            return@LaunchedEffect
        }
        if (imeVisible) {
            keyboardWasOpen = true
            suppressingImeClose = false
        } else if (keyboardWasOpen && !suppressingImeClose) {
            keyboardWasOpen = false
            onDismiss()
        }
    }

    // Back closes the overlay itself, never the dashboard behind it.
    BackHandler(enabled = visible) {
        keyboardController?.hide()
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) +
            scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ),
        exit = fadeOut(tween(140)) +
            scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(160, easing = FastOutSlowInEasing)
            ),
        modifier = modifier.fillMaxSize()
    ) {
        // The keys shade owns the top dock, exactly where the dashboard's device card
        // sits, so it replaces that component instead of stacking under it. The island
        // and the preview keep the bottom edge, right above the keyboard.
        InteractiveKeyShade(
            targetOs = detectedOs,
            heldModifiers = heldModifiers,
            onKey = sendCatalogKey,
            mapperKeys = mapperKeys,
            onMapperSlotEdit = { assigningSlotIndex = it },
            onToggleModifier = toggleModifier,
            onOpenChanged = { isKeyDrawerOpen = it },
            // Taller than the device dock so the mapper keys sit in the crest with room.
            dockHeight = 132.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview popup: modifiers being chained and the last keys sent. It
                    // grows out of the island and sinks back into it when it empties.
                    AnimatedVisibility(
                        visible = heldModifiers.isNotEmpty() || history.isNotEmpty(),
                        enter = fadeIn(tween(180)) +
                            expandVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Bottom
                            ) +
                            slideInVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 2 }
                            ),
                        exit = fadeOut(tween(120)) +
                            shrinkVertically(
                                animationSpec = tween(160, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.Bottom
                            ) +
                            slideOutVertically(
                                animationSpec = tween(160, easing = FastOutSlowInEasing),
                                targetOffsetY = { it / 2 }
                            )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            shadowElevation = 3.dp
                        ) {
                            InteractivePreviewChips(
                                heldModifiers = heldModifiers,
                                history = history,
                                targetOs = detectedOs,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // The keys themselves: the island that stands where the device card was.
                    KeyboardIsland(
                        detectedOs = detectedOs,
                        heldModifiers = heldModifiers,
                        mapperKeys = mapperKeys,
                        onToggleModifier = toggleModifier,
                        onKeyClick = sendCatalogKey,
                        onSlotEdit = { assigningSlotIndex = it },
                        onIslandClick = focusCaptureField,
                        hint = "Modifiers chain to the next key"
                    )
                }

                // Invisible capture field keeps the system IME attached while every
                // diff is forwarded over HID.
                BasicTextField(
                    value = captureValue,
                    onValueChange = { newValue ->
                        val oldText = captureValue.text
                        val newText = newValue.text
                        when {
                            newText.startsWith(oldText) -> {
                                newText.substring(oldText.length).forEach { char -> onCharTyped(char) }
                            }

                            oldText.startsWith(newText) -> {
                                repeat(oldText.length - newText.length) { onBackspace() }
                            }

                            else -> {
                                newText.removePrefix(OVERLAY_CAPTURE_SENTINEL).forEach { char -> onCharTyped(char) }
                            }
                        }
                        captureValue = overlaySentinelValue()
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

                if (assigningSlotIndex != null) {
                    val slotIdx = assigningSlotIndex!!
                    KeyPickerSheet(
                        slotIndex = slotIdx,
                        currentKey = InteractiveKeys.findByToken(mapperKeys.getOrElse(slotIdx) { "" }),
                        onSelectKey = { pickedKey ->
                            settingsRepository.updateInteractiveMapperSlot(slotIdx, pickedKey.token)
                            assigningSlotIndex = null
                        },
                        onClearKey = {
                            settingsRepository.updateInteractiveMapperSlot(slotIdx, "")
                            assigningSlotIndex = null
                        },
                        onDismiss = { assigningSlotIndex = null }
                    )
                }
            }
        }
    }
}

private const val OVERLAY_CAPTURE_SENTINEL = "\n"
private const val OVERLAY_TYPING_COOLDOWN_MS = 1200L
private const val OVERLAY_HISTORY_ITEMS = 6

private fun overlaySentinelValue() = TextFieldValue(
    text = OVERLAY_CAPTURE_SENTINEL,
    selection = TextRange(OVERLAY_CAPTURE_SENTINEL.length)
)

private fun labelForOverlayChar(char: Char): String = when (char) {
    ' ' -> "Space"
    '\n', '\r' -> "Enter"
    '\t' -> "Tab"
    else -> char.toString()
}
